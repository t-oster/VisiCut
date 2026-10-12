//! Test pattern for the engrave line shift. Bidirectional engraving fires
//! late, so each line lands shifted in its travel direction; the machine table
//! (`MachineSettings::engrave_shift`) moves every line back. The pattern
//! engraves, for each table speed 10 %, 20 %, …, 100 %, five blocks of
//! vertical bars with −2 … +2 pixels on top of the current table. The block
//! with the straightest bar edges shows the correction for that speed.
use crate::{
    i18n::tr,
    ltt::engrave_shift_px,
    project::{JobStep, MachineSettings, Operation, Project},
    raster::{Dithering, RasterSettings},
};

/// Table speeds, one pattern row each (top to bottom).
pub const SPEEDS: [f32; 10] = [10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0];
/// Extra shift of the blocks from left to right, in 500-DPI pixels.
pub const OFFSETS: [i32; 5] = [-2, -1, 0, 1, 2];
/// Block number (1-based, from the left) of the current table.
pub const CENTER_BLOCK: usize = 3;

const BLOCK_W: f64 = 8.0;
const BLOCK_H: f64 = 4.0;
const COLUMN_PITCH: f64 = 11.0;
const ROW_PITCH: f64 = 7.0;
/// Left of the blocks: one bar per row, 1 mm long per 10 % speed.
const LEFT: f64 = 13.0;
/// Above the blocks: a triangle over the middle column.
const TOP: f64 = 3.0;

fn width() -> f64 {
    LEFT + OFFSETS.len() as f64 * COLUMN_PITCH - (COLUMN_PITCH - BLOCK_W)
}

fn height() -> f64 {
    TOP + SPEEDS.len() as f64 * ROW_PITCH - (ROW_PITCH - BLOCK_H)
}

fn rect(x: f64, y: f64, w: f64, h: f64) -> String {
    format!("M{x} {y}h{w}v{h}h{}z", -w)
}

/// The pattern as SVG in millimetres and the object ids of each row:
/// row marker (with the triangle in the first row) and the five blocks.
/// Object ids of one pattern row: its markers and its blocks.
type Row = (Vec<usize>, Vec<usize>);

fn svg() -> (String, Vec<Row>) {
    let mut body = String::new();
    let mut ids = Vec::new();
    let mut next = 0;
    for (row, speed) in SPEEDS.iter().enumerate() {
        let y = TOP + row as f64 * ROW_PITCH;
        let mut marker = vec![next];
        body += &format!(
            r#"<path id="Tempo{speed}" d="{}"/>"#,
            rect(0.0, y + 1.25, *speed as f64 / 10.0, 1.5)
        );
        next += 1;
        if row == 0 {
            let x = LEFT + (CENTER_BLOCK - 1) as f64 * COLUMN_PITCH + BLOCK_W / 2.0;
            body += &format!(r#"<path id="Mitte" d="M{} 0h3l-1.5 2z"/>"#, x - 1.5);
            marker.push(next);
            next += 1;
        }
        let mut blocks = Vec::new();
        for column in 0..OFFSETS.len() {
            let x = LEFT + column as f64 * COLUMN_PITCH;
            let d: String = (0..8)
                .map(|bar| rect(x + bar as f64, y, 0.5, BLOCK_H))
                .collect();
            body += &format!(r#"<path id="Tempo{speed}-Block{}" d="{d}"/>"#, column + 1);
            blocks.push(next);
            next += 1;
        }
        ids.push((marker, blocks));
    }
    let (w, h) = (width(), height());
    (
        format!(
            r#"<svg xmlns="http://www.w3.org/2000/svg" width="{w}mm" height="{h}mm" viewBox="0 0 {w} {h}"><g fill="black" stroke="none">{body}</g></svg>"#
        ),
        ids,
    )
}

/// A project engraving the pattern with the given power; the machine
/// settings, target and material of `base` are kept. One step per block.
pub fn project(base: &Project, power_percent: f32) -> Project {
    let (svg, ids) = svg();
    let raster = RasterSettings {
        dithering: Dithering::Threshold,
        invert: false,
        color_shift: 0,
        bidirectional: true,
        bottom_up: false,
    };
    let mut steps = Vec::new();
    for (speed, (marker, blocks)) in SPEEDS.iter().zip(ids) {
        for (column, (offset, block)) in OFFSETS.iter().zip(blocks).enumerate() {
            let mut objects = vec![block];
            if column + 1 == CENTER_BLOCK {
                objects.extend(&marker);
            }
            steps.push(JobStep {
                objects,
                power_percent,
                speed_percent: *speed,
                raster,
                engrave_shift_px: *offset,
                ..JobStep::new(Operation::Engrave)
            });
        }
    }
    Project {
        name: tr("Zeilenversatz-Test").into(),
        svg,
        // Room for the overscan of 35 mm at full speed on both sides.
        x_mm: 50.0,
        y_mm: 20.0,
        width_mm: width() as f32,
        height_mm: height() as f32,
        operation: Operation::Engrave,
        power_percent,
        speed_percent: 100.0,
        passes: 1,
        steps,
        rotary_axis: false,
        raster,
        ignore_filters: Vec::new(),
        ..base.clone()
    }
}

/// Table value (1/4000 inch) that the driver turns into exactly `shift_px`.
fn table_value(shift_px: i32) -> f32 {
    match shift_px {
        s if s > 0 => -8.0 * (s + 1) as f32,
        0 => -4.0,
        s => -8.0 * s as f32,
    }
}

/// The machine settings after the test: `best` holds the chosen block
/// (1 … 5, from the left) of each speed row; the middle block keeps a value.
pub fn apply(machine: &MachineSettings, best: &[usize; 10]) -> Result<MachineSettings, String> {
    let mut result = machine.clone();
    for (i, block) in best.iter().enumerate() {
        let offset = *OFFSETS
            .get(block.wrapping_sub(1))
            .ok_or(tr("Block muss zwischen 1 und 5 liegen"))?;
        if offset != 0 {
            let shift = engrave_shift_px(machine, SPEEDS[i], offset);
            result.engrave_shift[i] = table_value(shift);
        }
    }
    result.validate()?;
    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ltt;

    #[test]
    fn table_values_give_exact_pixel_shifts() {
        let machine = MachineSettings::default();
        for shift in -6..=6 {
            let mut m = machine.clone();
            m.engrave_shift = [table_value(shift); 10];
            for speed in SPEEDS {
                assert_eq!(engrave_shift_px(&m, speed, 0), shift);
            }
        }
    }

    #[test]
    fn applying_blocks_moves_each_speed_by_its_offset() {
        let machine = MachineSettings::default();
        let best = [1, 2, 3, 4, 5, 3, 3, 1, 5, 2];
        let new = apply(&machine, &best).unwrap();
        for (i, speed) in SPEEDS.iter().enumerate() {
            let offset = OFFSETS[best[i] - 1];
            assert_eq!(
                engrave_shift_px(&new, *speed, 0),
                engrave_shift_px(&machine, *speed, 0) + offset
            );
            if offset == 0 {
                assert_eq!(new.engrave_shift[i], machine.engrave_shift[i]);
            }
        }
        assert!(apply(&machine, &[0; 10]).is_err());
        assert!(apply(&machine, &[6; 10]).is_err());
    }

    #[test]
    fn pattern_prepares_one_part_per_block_with_its_shift() {
        let project = project(&Project::default(), 30.0);
        assert_eq!(project.steps.len(), SPEEDS.len() * OFFSETS.len());
        project.validate().unwrap();
        let prepared = ltt::prepare(&project).unwrap();
        assert_eq!(prepared.steps.len(), 50);
        assert!(
            prepared
                .warnings
                .iter()
                .any(|w| w.contains("Zeilenversatz"))
        );
        // Blocks of one row differ only by their shift.
        let mut plain = project.clone();
        plain.steps.iter_mut().for_each(|s| s.engrave_shift_px = 0);
        assert_ne!(
            ltt::prepare(&plain).unwrap().jobs[0].bytes,
            prepared.jobs[0].bytes
        );
    }
}
