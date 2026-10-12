//! Port of VisiCut's tools/cameraserver: answers every HTTP request with a
//! fresh photo taken by an external capture command, e.g.
//!
//!   visicut-cameraserver -- gphoto2 --capture-image-and-download --stdout
//!   visicut-cameraserver --output snapshot.jpg -- imagesnap -q snapshot.jpg
//!
//! Set the device's camera URL to http://<host>:8088/visicam.jpg.
use std::{
    io::{BufRead, BufReader, Write},
    net::{TcpListener, TcpStream},
    process::Command,
    time::Duration,
};
use visicut_core::{i18n::tr, tr};

struct Options {
    bind: String,
    port: u16,
    rotate: u16,
    output: Option<String>,
    command: Vec<String>,
}

fn usage() -> &'static str {
    tr(
        "Aufruf: visicut-cameraserver [--bind ADRESSE] [--port 8088] \
        [--rotate 0|90|180|270] [--output DATEI] -- AUFNAHMEBEFEHL [ARGUMENTE …]\n\
        Ohne --output muss der Befehl das Bild (JPEG oder PNG) auf stdout schreiben.",
    )
}

fn parse(args: impl Iterator<Item = String>) -> Result<Options, String> {
    let mut options = Options {
        bind: "0.0.0.0".into(),
        port: 8088,
        rotate: 0,
        output: None,
        command: Vec::new(),
    };
    let mut args = args.peekable();
    while let Some(arg) = args.next() {
        let mut value = || {
            args.next()
                .ok_or_else(|| tr!("{option} benötigt einen Wert", option = arg))
        };
        match arg.as_str() {
            "--bind" => options.bind = value()?,
            "--port" => options.port = value()?.parse().map_err(|_| tr("Ungültiger Port"))?,
            "--rotate" => {
                options.rotate = value()?.parse().map_err(|_| tr("Ungültige Drehung"))?;
                if ![0, 90, 180, 270].contains(&options.rotate) {
                    return Err(tr("Drehung muss 0, 90, 180 oder 270 sein").into());
                }
            }
            "--output" => options.output = Some(value()?),
            "--" => options.command = args.by_ref().collect(),
            "-h" | "--help" => return Err(usage().into()),
            other => {
                return Err(format!(
                    "{}\n{}",
                    tr!("Unbekannte Option {option}", option = other),
                    usage()
                ));
            }
        }
    }
    if options.command.is_empty() {
        return Err(usage().into());
    }
    Ok(options)
}

fn capture(options: &Options) -> Result<Vec<u8>, String> {
    if let Some(output) = &options.output {
        let _ = std::fs::remove_file(output);
    }
    let result = Command::new(&options.command[0])
        .args(&options.command[1..])
        .output()
        .map_err(|e| format!("{}: {e}", options.command[0]))?;
    if !result.status.success() {
        return Err(tr!(
            "Aufnahmebefehl fehlgeschlagen ({status}): {error}",
            status = result.status,
            error = String::from_utf8_lossy(&result.stderr).trim()
        ));
    }
    match &options.output {
        Some(output) => std::fs::read(output).map_err(|e| format!("{output}: {e}")),
        None => Ok(result.stdout),
    }
}

/// Returns the response body and content type, rotating like `mogrify -rotate`.
fn image_response(bytes: Vec<u8>, rotate: u16) -> Result<(Vec<u8>, &'static str), String> {
    let format = image::guess_format(&bytes).map_err(|_| tr("Aufnahme ist kein Bild"))?;
    if rotate == 0 {
        return Ok((bytes, format.to_mime_type()));
    }
    let picture = image::load_from_memory(&bytes).map_err(|e| e.to_string())?;
    let rotated = match rotate {
        90 => picture.rotate90(),
        180 => picture.rotate180(),
        _ => picture.rotate270(),
    };
    let mut jpeg = Vec::new();
    rotated
        .to_rgb8()
        .write_to(
            &mut std::io::Cursor::new(&mut jpeg),
            image::ImageFormat::Jpeg,
        )
        .map_err(|e| e.to_string())?;
    Ok((jpeg, "image/jpeg"))
}

fn handle(
    stream: TcpStream,
    take_photo: impl FnOnce() -> Result<Vec<u8>, String>,
    rotate: u16,
) -> std::io::Result<()> {
    stream.set_read_timeout(Some(Duration::from_secs(10)))?;
    let mut reader = BufReader::new(stream.try_clone()?);
    let mut line = String::new();
    // Consume the request head; the path does not matter.
    let mut total = 0;
    loop {
        line.clear();
        let n = reader.read_line(&mut line)?;
        total += n;
        if n == 0 || line == "\r\n" || line == "\n" || total > 16 * 1024 {
            break;
        }
    }
    let mut stream = stream;
    match take_photo().and_then(|bytes| image_response(bytes, rotate)) {
        Ok((body, content_type)) => {
            write!(
                stream,
                "HTTP/1.0 200 OK\r\nContent-Type: {content_type}\r\nContent-Length: {}\r\n\
                 Cache-Control: no-store\r\nConnection: close\r\n\r\n",
                body.len()
            )?;
            stream.write_all(&body)?;
        }
        Err(error) => {
            eprintln!("{error}");
            write!(
                stream,
                "HTTP/1.0 500 Internal Server Error\r\nContent-Type: text/plain; charset=utf-8\r\n\
                 Content-Length: {}\r\nConnection: close\r\n\r\n{error}",
                error.len()
            )?;
        }
    }
    stream.flush()
}

fn main() {
    // Ohne Einstellungen der Oberfläche gilt die Systemsprache.
    visicut_core::i18n::set_language(visicut_core::i18n::Language::system());
    let options = match parse(std::env::args().skip(1)) {
        Ok(options) => options,
        Err(message) => {
            eprintln!("{message}");
            std::process::exit(2);
        }
    };
    let listener = match TcpListener::bind((options.bind.as_str(), options.port)) {
        Ok(listener) => listener,
        Err(error) => {
            eprintln!("{}:{}: {error}", options.bind, options.port);
            std::process::exit(1);
        }
    };
    println!(
        "{}",
        tr!(
            "Kameraserver läuft. Kamera-URL in VisiCutRust: \
             http://<dieser Rechner>:{port}/visicam.jpg",
            port = options.port
        )
    );
    // One request at a time: a camera can only take one photo at once.
    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                if let Err(error) = handle(stream, || capture(&options), options.rotate) {
                    eprintln!("{}", tr!("Anfrage fehlgeschlagen: {error}", error = error));
                }
            }
            Err(error) => eprintln!(
                "{}",
                tr!("Verbindung fehlgeschlagen: {error}", error = error)
            ),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Read;

    fn png(width: u32, height: u32) -> Vec<u8> {
        let mut bytes = Vec::new();
        image::RgbImage::new(width, height)
            .write_to(
                &mut std::io::Cursor::new(&mut bytes),
                image::ImageFormat::Png,
            )
            .unwrap();
        bytes
    }

    fn request(
        take_photo: impl FnOnce() -> Result<Vec<u8>, String> + Send + 'static,
        rotate: u16,
    ) -> Vec<u8> {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let port = listener.local_addr().unwrap().port();
        let server = std::thread::spawn(move || {
            let (stream, _) = listener.accept().unwrap();
            handle(stream, take_photo, rotate).unwrap();
        });
        let mut client = TcpStream::connect(("127.0.0.1", port)).unwrap();
        client
            .write_all(b"GET /visicam.jpg HTTP/1.1\r\nHost: x\r\n\r\n")
            .unwrap();
        let mut response = Vec::new();
        client.read_to_end(&mut response).unwrap();
        server.join().unwrap();
        response
    }

    fn body(response: &[u8]) -> &[u8] {
        let split = response.windows(4).position(|w| w == b"\r\n\r\n").unwrap();
        &response[split + 4..]
    }

    #[test]
    fn serves_captured_image_unchanged_or_rotated_as_jpeg() {
        let response = request(|| Ok(png(4, 2)), 0);
        assert!(response.starts_with(b"HTTP/1.0 200 OK\r\nContent-Type: image/png"));
        assert_eq!(body(&response), png(4, 2));

        let response = request(|| Ok(png(4, 2)), 90);
        assert!(response.starts_with(b"HTTP/1.0 200 OK\r\nContent-Type: image/jpeg"));
        let rotated = image::load_from_memory(body(&response)).unwrap();
        assert_eq!((rotated.width(), rotated.height()), (2, 4));
    }

    #[test]
    fn reports_capture_errors_and_parses_options() {
        let response = request(|| Err("Kamera fehlt".into()), 0);
        assert!(response.starts_with(b"HTTP/1.0 500"));
        assert_eq!(body(&response), "Kamera fehlt".as_bytes());

        let args = [
            "--port", "9000", "--rotate", "180", "--", "gphoto2", "--stdout",
        ];
        let options = parse(args.iter().map(|s| s.to_string())).unwrap();
        assert_eq!((options.port, options.rotate), (9000, 180));
        assert_eq!(options.command, ["gphoto2", "--stdout"]);
        assert!(parse(["--rotate", "45", "--", "x"].iter().map(|s| s.to_string())).is_err());
        assert!(parse(std::iter::empty()).is_err());
    }
}
