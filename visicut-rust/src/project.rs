use crate::{mapping::Filter, raster::RasterSettings};
use serde::{Deserialize, Serialize};

#[derive(Clone, Serialize, Deserialize)]
#[serde(try_from = "ProjectFile")]
pub struct Project {
    pub format_version: u32,
    pub name: String,
    pub svg: String,
    pub bed_width_mm: f32,
    pub bed_height_mm: f32,
    pub x_mm: f32,
    pub y_mm: f32,
    pub width_mm: f32,
    pub height_mm: f32,
    pub material: String,
    pub thickness_mm: f32,
    pub operation: Operation,
    pub power_percent: f32,
    pub speed_percent: f32,
    pub passes: u32,
    #[serde(default = "default_host")]
    pub hostname: String,
    #[serde(default = "default_port")]
    pub port: u16,
    pub steps: Vec<JobStep>,
    /// Rotary engraving: Y becomes the rotation of a cylinder of this diameter.
    #[serde(default)]
    pub rotary_axis: bool,
    #[serde(default = "default_rotary_diameter")]
    pub rotary_diameter_mm: f32,
    /// Raster options when the whole motif is engraved (no steps).
    #[serde(default = "RasterSettings::legacy")]
    pub raster: RasterSettings,
    /// Objects matching any of these filter sets are excluded from rest steps
    /// (VisiCut mappings to "ignore").
    #[serde(default)]
    pub ignore_filters: Vec<Vec<Filter>>,
    /// Order of vector paths; anything but the default is experimental.
    #[serde(default)]
    pub cut_order: CutOrder,
}

/// Order of the vector paths of a step.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CutOrder {
    /// VisiCut's "inner first", byte for byte like Java (tests/java_parity).
    #[default]
    VisiCut,
    /// Experimental: still inner first, but always on to the nearest allowed
    /// path; closed contours may start at any vertex.
    ShortestTravel,
}

/// Further power/speed settings applied to the same objects afterwards
/// (several LaserProperty entries of one VisiCut profile).
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct ParameterSet {
    pub power_percent: f32,
    pub speed_percent: f32,
    pub passes: u32,
}

/// A processing step. Objects are selected explicitly (`objects`), by
/// filters (`filters`, an empty list selects everything) or as the rest not
/// selected by any other step (`rest`). No steps: the whole SVG is one job.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct JobStep {
    pub operation: Operation,
    pub objects: Vec<usize>,
    pub power_percent: f32,
    pub speed_percent: f32,
    pub passes: u32,
    #[serde(default)]
    pub filters: Option<Vec<Filter>>,
    #[serde(default)]
    pub rest: bool,
    #[serde(default = "RasterSettings::legacy")]
    pub raster: RasterSettings,
    #[serde(default)]
    pub additional: Vec<ParameterSet>,
}

impl JobStep {
    pub fn new(operation: Operation) -> Self {
        Self {
            operation,
            objects: Vec::new(),
            power_percent: 20.0,
            speed_percent: 100.0,
            passes: 1,
            filters: None,
            rest: false,
            raster: RasterSettings::default(),
            additional: Vec::new(),
        }
    }

    /// All parameter sets in execution order.
    pub fn parameters(&self) -> Vec<ParameterSet> {
        std::iter::once(ParameterSet {
            power_percent: self.power_percent,
            speed_percent: self.speed_percent,
            passes: self.passes,
        })
        .chain(self.additional.iter().cloned())
        .collect()
    }
}

impl ParameterSet {
    fn validate(&self) -> Result<(), String> {
        if !self.power_percent.is_finite()
            || !(0.0..=100.0).contains(&self.power_percent)
            || !self.speed_percent.is_finite()
            || !(0.1..=100.0).contains(&self.speed_percent)
            || !(1..=100).contains(&self.passes)
        {
            return Err("Ungültige Parameter in einem Bearbeitungsschritt".into());
        }
        Ok(())
    }
}

// V1 stored a derived physical speed. V2 stores the controller percentage;
// read both explicitly so legacy values never silently change units.
#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct ProjectFile {
    format_version: u32,
    name: String,
    svg: String,
    bed_width_mm: f32,
    bed_height_mm: f32,
    x_mm: f32,
    y_mm: f32,
    width_mm: f32,
    height_mm: f32,
    material: String,
    thickness_mm: f32,
    operation: Operation,
    power_percent: f32,
    speed_percent: Option<f32>,
    speed_mm_s: Option<f32>,
    passes: u32,
    #[serde(default = "default_host")]
    hostname: String,
    #[serde(default = "default_port")]
    port: u16,
    #[serde(default)]
    steps: Vec<JobStep>,
    #[serde(default)]
    rotary_axis: bool,
    #[serde(default = "default_rotary_diameter")]
    rotary_diameter_mm: f32,
    #[serde(default = "RasterSettings::legacy")]
    raster: RasterSettings,
    #[serde(default)]
    ignore_filters: Vec<Vec<Filter>>,
    #[serde(default)]
    cut_order: CutOrder,
}

impl TryFrom<ProjectFile> for Project {
    type Error = String;

    fn try_from(file: ProjectFile) -> Result<Self, Self::Error> {
        let speed_percent = match (file.format_version, file.speed_percent, file.speed_mm_s) {
            (2, Some(percent), None) => percent,
            (1, None, Some(mm_s)) => {
                let factor = if file.operation == Operation::Engrave {
                    6.4
                } else {
                    1.0
                };
                mm_s / (338.677 * factor) * 100.0
            }
            (1 | 2, _, _) => {
                return Err("Geschwindigkeitseinheit passt nicht zur Projektversion".into());
            }
            _ => return Err("Unbekannte Projektversion".into()),
        };
        Ok(Self {
            format_version: 2,
            name: file.name,
            svg: file.svg,
            bed_width_mm: file.bed_width_mm,
            bed_height_mm: file.bed_height_mm,
            x_mm: file.x_mm,
            y_mm: file.y_mm,
            width_mm: file.width_mm,
            height_mm: file.height_mm,
            material: file.material,
            thickness_mm: file.thickness_mm,
            operation: file.operation,
            power_percent: file.power_percent,
            speed_percent,
            passes: file.passes,
            hostname: file.hostname,
            port: file.port,
            steps: file.steps,
            rotary_axis: file.rotary_axis,
            rotary_diameter_mm: file.rotary_diameter_mm,
            raster: file.raster,
            ignore_filters: file.ignore_filters,
            cut_order: file.cut_order,
        })
    }
}

fn default_host() -> String {
    "lasercutter2".into()
}
fn default_port() -> u16 {
    9100
}
// VisiCut's default rotary diameter.
fn default_rotary_diameter() -> f32 {
    100.0
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Operation {
    Cut,
    Engrave,
    Mark,
    /// Greyscale depth engraving (VisiCut Raster3dProfile).
    Engrave3d,
}

impl Operation {
    pub const ALL: [Operation; 4] = [Self::Engrave, Self::Engrave3d, Self::Mark, Self::Cut];
    pub fn prefix(self) -> &'static str {
        match self {
            Self::Cut => "Cut",
            Self::Engrave => "Engrav",
            Self::Engrave3d => "Eng3D",
            Self::Mark => "Mark",
        }
    }
    pub fn order(self) -> u8 {
        match self {
            Self::Engrave => 0,
            Self::Engrave3d => 1,
            Self::Mark => 2,
            Self::Cut => 3,
        }
    }
    pub fn is_raster(self) -> bool {
        matches!(self, Self::Engrave | Self::Engrave3d)
    }
}

impl Default for Project {
    fn default() -> Self {
        Self {
            format_version: 2,
            name: "Neues Projekt".into(),
            svg: String::new(),
            bed_width_mm: 1000.0,
            bed_height_mm: 600.0,
            x_mm: 10.0,
            y_mm: 10.0,
            width_mm: 100.0,
            height_mm: 60.0,
            material: "Material wählen".into(),
            thickness_mm: 3.0,
            operation: Operation::Cut,
            power_percent: 20.0,
            speed_percent: 10.0,
            passes: 1,
            hostname: default_host(),
            port: default_port(),
            steps: Vec::new(),
            rotary_axis: false,
            rotary_diameter_mm: default_rotary_diameter(),
            raster: RasterSettings::default(),
            ignore_filters: Vec::new(),
            cut_order: CutOrder::VisiCut,
        }
    }
}

impl Project {
    pub fn validate(&self) -> Result<(), String> {
        self.validate_document()?;
        if self.x_mm < 0.0
            || self.y_mm < 0.0
            || self.x_mm + self.width_mm > self.bed_width_mm + 0.001
            || self.y_mm + self.height_mm > self.bed_height_mm + 0.001
        {
            return Err("Das Motiv liegt außerhalb des Arbeitsbetts".into());
        }
        if self.svg.is_empty() {
            return Err("Zuerst eine SVG-Datei importieren".into());
        }
        Ok(())
    }

    /// Documents may contain unfinished placement or no artwork. Sending has
    /// stricter requirements; drafts still need to survive a save/open cycle.
    pub fn validate_document(&self) -> Result<(), String> {
        if self.format_version != 2 {
            return Err("Unbekannte Projektversion".into());
        }
        for (name, value) in [
            ("Bettbreite", self.bed_width_mm),
            ("Betthöhe", self.bed_height_mm),
            ("Motivbreite", self.width_mm),
            ("Motivhöhe", self.height_mm),
        ] {
            if !value.is_finite() || value <= 0.0 || value > 100_000.0 {
                return Err(format!("{name} muss positiv und höchstens 100000 sein"));
            }
        }
        if !self.thickness_mm.is_finite() || !(0.0..=100_000.0).contains(&self.thickness_mm) {
            return Err("Materialstärke muss nichtnegativ und endlich sein".into());
        }
        if !self.power_percent.is_finite() || !(0.0..=100.0).contains(&self.power_percent) {
            return Err("Leistung muss zwischen 0 und 100 % liegen".into());
        }
        if !self.speed_percent.is_finite() || !(0.1..=100.0).contains(&self.speed_percent) {
            return Err("Geschwindigkeit muss zwischen 0,1 und 100 % liegen".into());
        }
        if !(1..=100).contains(&self.passes) {
            return Err("Durchgänge müssen zwischen 1 und 100 liegen".into());
        }
        // The LTT driver requires at least 5 mm and stores the radius in 0.01 mm.
        if !self.rotary_diameter_mm.is_finite()
            || !(5.0..=1000.0).contains(&self.rotary_diameter_mm)
        {
            return Err("Durchmesser für die Drehachse muss zwischen 5 und 1000 mm liegen".into());
        }
        if !self.x_mm.is_finite() || !self.y_mm.is_finite() {
            return Err("Position muss endlich sein".into());
        }
        self.raster.validate()?;
        if self.steps.len() > 64 {
            return Err("Höchstens 64 Bearbeitungsschritte sind zulässig".into());
        }
        let count = if self.steps.iter().any(|s| s.filters.is_none() && !s.rest) {
            crate::selection::objects(&self.svg)?.len()
        } else {
            0
        };
        for step in &self.steps {
            if step.additional.len() > 15 {
                return Err("Höchstens 16 Parametersätze je Schritt".into());
            }
            for set in step.parameters() {
                set.validate()?;
            }
            step.raster.validate()?;
            if (step.rest || step.filters.is_some()) && !step.objects.is_empty() {
                return Err(
                    "Regelschritte dürfen keine einzeln gewählten Objekte enthalten".into(),
                );
            }
            if step.rest && step.filters.is_some() {
                return Err("Ein Restschritt hat keine Filter".into());
            }
            for filter in step
                .filters
                .iter()
                .flatten()
                .chain(self.ignore_filters.iter().flatten())
            {
                filter.validate()?;
            }
            if step.objects.iter().any(|id| *id >= count) {
                return Err("Zuordnung verweist auf ein nicht vorhandenes SVG-Objekt".into());
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn project() -> Project {
        Project {
            svg: "<svg/>".into(),
            ..Default::default()
        }
    }
    #[test]
    fn rejects_out_of_bed_and_nan() {
        let mut p = project();
        assert!(p.validate().is_ok());
        p.x_mm = 950.0;
        assert!(p.validate().is_err());
        p.x_mm = f32::NAN;
        assert!(p.validate().is_err());
    }
    #[test]
    fn rejects_unknown_version_and_invalid_parameters() {
        let mut p = project();
        p.format_version = 3;
        assert!(p.validate().is_err());
        p.format_version = 2;
        p.power_percent = 101.0;
        assert!(p.validate().is_err());
    }
    #[test]
    fn project_roundtrip_preserves_embedded_svg() {
        let p = project();
        let loaded: Project = serde_json::from_str(&serde_json::to_string(&p).unwrap()).unwrap();
        assert_eq!(loaded.svg, p.svg);
        assert!(loaded.validate().is_ok());
    }

    #[test]
    fn loads_legacy_v2_without_object_steps() {
        let mut value = serde_json::to_value(project()).unwrap();
        value.as_object_mut().unwrap().remove("steps");
        let loaded: Project = serde_json::from_value(value).unwrap();
        assert!(loaded.steps.is_empty());
        assert!(loaded.validate().is_ok());
    }

    #[test]
    fn drafts_can_save_without_being_sendable() {
        let p = Project::default();
        assert!(p.validate_document().is_ok());
        assert!(p.validate().is_err());
        let p = Project {
            x_mm: 950.0,
            thickness_mm: 0.0,
            ..project()
        };
        assert!(p.validate_document().is_ok());
        assert!(p.validate().is_err());
    }

    #[test]
    fn migrates_legacy_speed_with_operation_specific_scale() {
        for (operation, old_speed, expected) in [
            (Operation::Cut, 30.48093, 9.0),
            (Operation::Engrave, 2167.5328, 100.0),
        ] {
            let mut file = serde_json::to_value(Project {
                operation,
                ..project()
            })
            .unwrap();
            file["format_version"] = 1.into();
            file.as_object_mut().unwrap().remove("speed_percent");
            file["speed_mm_s"] = serde_json::json!(old_speed);
            let loaded: Project = serde_json::from_value(file).unwrap();
            assert_eq!(loaded.format_version, 2);
            assert!((loaded.speed_percent - expected).abs() < 0.0001);
            let saved = serde_json::to_value(loaded).unwrap();
            assert!(saved.get("speed_mm_s").is_none());
        }
    }

    #[test]
    fn rejects_invalid_percent_and_ambiguous_unit() {
        for speed in [0.0, -1.0, 100.1, f32::NAN] {
            assert!(
                Project {
                    speed_percent: speed,
                    ..project()
                }
                .validate_document()
                .is_err()
            );
        }
        let mut file = serde_json::to_value(project()).unwrap();
        file["speed_mm_s"] = serde_json::json!(10.0);
        assert!(serde_json::from_value::<Project>(file).is_err());
    }
}
