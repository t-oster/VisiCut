//! Sandboxed JavaScript for LaserScript files and parametric SVG expressions.
//!
//! VisiCut runs both through Rhino. Here the pure-Rust engine Boa evaluates
//! them on a separate thread. Boa's core has no file, network or process
//! access; only the functions registered by the caller are reachable. Every
//! run gets a wall-clock limit (checked while the script executes), a loop
//! iteration limit per function call and a recursion limit. Callers bound the
//! size of what a script may produce.
use crate::{i18n::tr, tr};
use boa_engine::{Context, JsError, JsValue, Script, Source};
use std::future::Future;
use std::sync::mpsc;
use std::task::Poll;
use std::time::{Duration, Instant};

/// Wall-clock limit of one import's scripts.
pub const TIME_LIMIT: Duration = Duration::from_secs(5);
/// Loop iterations per function call; ends simple endless loops even where
/// the clock is not checked (inside callbacks of built-in functions).
const LOOP_LIMIT: u64 = 20_000_000;
const RECURSION_LIMIT: usize = 400;
/// VM "cycles" between two clock checks.
const BUDGET: u32 = 20_000;
/// Extra time the caller waits for a worker that does not stop by itself.
const GRACE: Duration = Duration::from_secs(1);

pub struct Engine {
    pub context: Context,
    deadline: Instant,
    limit: Duration,
    aborted: bool,
}

impl Engine {
    fn new(limit: Duration) -> Self {
        let mut context = Context::default();
        let limits = context.runtime_limits_mut();
        limits.set_loop_iteration_limit(LOOP_LIMIT);
        limits.set_recursion_limit(RECURSION_LIMIT);
        Self {
            context,
            deadline: Instant::now() + limit,
            limit,
            aborted: false,
        }
    }

    /// Runs `code` as a script and returns its completion value.
    pub fn eval(&mut self, code: &str) -> Result<JsValue, String> {
        if self.aborted || Instant::now() > self.deadline {
            self.aborted = true;
            return Err(timeout_message(self.limit));
        }
        let script = Script::parse(Source::from_bytes(code), None, &mut self.context)
            .map_err(|e| describe(&e))?;
        let deadline = self.deadline;
        let result = {
            let future = script.evaluate_async_with_budget(&mut self.context, BUDGET);
            let mut future = std::pin::pin!(future);
            let mut cx = std::task::Context::from_waker(std::task::Waker::noop());
            loop {
                match future.as_mut().poll(&mut cx) {
                    Poll::Ready(result) => break Some(result),
                    Poll::Pending if Instant::now() > deadline => break None,
                    Poll::Pending => {}
                }
            }
        };
        match result {
            Some(result) => result.map_err(|e| describe(&e)),
            None => {
                // The interrupted script left its frame on the VM stack.
                self.aborted = true;
                Err(timeout_message(self.limit))
            }
        }
    }

    /// Converts `value` like JavaScript's `String(value)`.
    pub fn string(&mut self, value: &JsValue) -> Result<String, String> {
        value
            .to_string(&mut self.context)
            .map(|s| s.to_std_string_escaped())
            .map_err(|e| describe(&e))
    }
}

/// Runs `job` with a fresh engine on its own thread and waits at most
/// `limit` (plus a grace period) for the result.
pub fn run_isolated<R, F>(limit: Duration, job: F) -> Result<R, String>
where
    R: Send + 'static,
    F: FnOnce(&mut Engine) -> Result<R, String> + Send + 'static,
{
    let (sender, receiver) = mpsc::channel();
    std::thread::Builder::new()
        .name("visicut-script".into())
        .stack_size(16 * 1024 * 1024)
        .spawn(move || {
            let mut engine = Engine::new(limit);
            let _ = sender.send(job(&mut engine));
        })
        .map_err(|e| tr!("Skript konnte nicht gestartet werden: {error}", error = e))?;
    match receiver.recv_timeout(limit + GRACE) {
        Ok(result) => result,
        // The worker is left behind; Boa cannot be interrupted from outside.
        Err(mpsc::RecvTimeoutError::Timeout) => Err(timeout_message(limit)),
        Err(mpsc::RecvTimeoutError::Disconnected) => {
            Err(tr("Skriptausführung ist unerwartet abgebrochen").into())
        }
    }
}

pub fn timeout_message(limit: Duration) -> String {
    tr!(
        "Skript nach {seconds} s abgebrochen (Endlosschleife oder zu aufwendig)",
        seconds = limit.as_secs_f32()
    )
}

pub fn describe(error: &JsError) -> String {
    if error.as_native().is_some_and(|e| e.is_runtime_limit()) {
        return tr!(
            "Skript abgebrochen: Laufzeitgrenze überschritten ({error})",
            error = error
        );
    }
    tr!("Skriptfehler: {error}", error = error)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn evaluates_javascript() {
        let value = run_isolated(TIME_LIMIT, |engine| {
            let value = engine.eval("var a = 2; a * 21")?;
            engine.string(&value)
        })
        .unwrap();
        assert_eq!(value, "42");
    }

    #[test]
    fn stops_endless_loops_by_time() {
        let error = run_isolated(Duration::from_millis(300), |engine| {
            engine
                .eval("var i = 0; while (true) { i = (i + 1) % 7; }")
                .map(|_| ())
        })
        .unwrap_err();
        assert!(error.contains("abgebrochen"), "{error}");
    }

    #[test]
    fn stops_endless_recursion() {
        let error = run_isolated(TIME_LIMIT, |engine| {
            engine.eval("function f() { return f(); } f()").map(|_| ())
        })
        .unwrap_err();
        assert!(error.contains("Skript"), "{error}");
    }

    #[test]
    fn has_no_host_access() {
        let value = run_isolated(TIME_LIMIT, |engine| {
            let value = engine.eval(
                "[typeof require, typeof fetch, typeof XMLHttpRequest, typeof process, typeof java, typeof Packages].join()",
            )?;
            engine.string(&value)
        })
        .unwrap();
        assert!(value.split(',').all(|t| t == "undefined"), "{value}");
    }
}
