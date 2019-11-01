All these files should, when loaded for profile "engrave all", show a rectangle (with or without stroke), that - including the outline - has exactly 100x100mm bounding box.

The outline stroke width is 10mm (with relative error < 1e-4), except for files with _none_, where there is no stroke at all.

TODO: write an automated unit-test for this.

