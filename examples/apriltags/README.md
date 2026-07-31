# Json AprilTags
---
Documentation and examples for the AprilTag module for Marrow. This module enables the loading and parsing of custom AprilTag family configurations from JSON files for use within FTC robot projects.

## Overview & Scheme Description
The `JsonApriltagFamily` class parses a JSON configuration file containing a list of AprilTag definitions. Each entry supports flexible positioning, orientation, and unit schemes to accommodate various coordinate systems and data formats.

### JSON Example
Here is an example JSON for the DECODE 2025-2026 season, showing all different supported schemes

``` json
[ 
  { // Objects for 'position' and 'orientation' with text unit. 
    "id": 20,
    "name": "BlueTarget",
    "size": 6.5,
    "position": {
      "x": -82.372696,
      "y": -78.6425,
      "z": 29.5
    },
    "unit": "inch",
    "orientation": {
      "w": 0.6725937,
      "x": -0.6725937,
      "y": -0.2182149,
      "z": 0.2182149,
      "acquisitionTime": 0
    }
  },
  { // Arrays for 'position' and 'orientation' without ordinal unit. 
    "id": 24,
    "name": "RedTarget",
    "size": 6.5,
    "position": [-82.372696, 79.6425, 29.5],
    "unit": 3,
    "orientation": [0.6725937, -0.6725937, -0.2182149, 0.2182149, 0]
  },
  { // Null for 'position' and 'orientation'. 
    "id": 21,
    "name": "Obelisk_GPP",
    "size": 6.5,
    "position": null,
    "unit": 3,
    "orientation": null
  },
  { // Empty 'position' and 'orientation' with ordinal unit. 
    "id": 22,
    "name": "Obelisk_PGP",
    "size": 6.5,
    "unit": 3
  },
  { // Empty 'position' and 'orientation' with text unit. 
    "id": 23,
    "name": "Obelisk_PPG",
    "size": 6.5,
    "unit": "in"
  }
]
```
## Example OpMode
This isn't a full OpMode, but it shows how to use the JsonApriltagFamily class.

```java
public class ApriltagJsonExampleOpMode extends OpMode {
    AprilTagProcessor aprilTagProcessor;

    @Override
    public void init() {
        // Init code //
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setTagLibrary(
                        new JsonApriltagFamily("Decode-2025-2026.json").getFamily()
                )
                .build();
        // More Init code //
    }

    @Override
    public void loop() {
        // loop code
    }
}
```

## JSON Specification

Units can be assigned using one of the following ways:

| Unit       	 | Short 	 | Long       	 | Ordinal 	 |
|--------------|---------|--------------|-----------|
| Meter      	 | m     	 | meter      	 | 0       	 |
| CentiMeter 	 | cm    	 | centimeter 	 | 1       	 |
| MilliMeter 	 | mm    	 | millimeter 	 | 2       	 |
| Inch       	 | in    	 | inch       	 | 3       	 |

- If the parsing of the 'Unit' field fails for some reason, the parser defaults to Inches.
- Position and Orientation are optional and can be omitted or set to null, but will be parsed to default values to ensure the data is valid.
    - Position vector will be `[0, 0, 0]`
    - Orientation quaternion will be `[1, 0, 0, 0, 0]`
- Positon vector can be either a 3 number array OR an object of the structure

| Factor 	 | Object Key 	 | Array Index 	 | Type  	 |
|----------|--------------|---------------|---------|
| X      	 | x          	 | 0           	 | float 	 |
| Y      	 | y          	 | 1           	 | float 	 |
| Z      	 | z          	 | 2           	 | float 	 |

- Orientation quaternion can be either a 5 number array OR an object of the structure

| Factor          	 | Object Key      	 | Array Index 	 | Type  	 |
|-------------------|-------------------|---------------|---------|
| W               	 | w               	 | 0           	 | float 	 |
| X               	 | x               	 | 1           	 | float 	 |
| Y               	 | Y               	 | 2           	 | float 	 |
| Z               	 | z               	 | 3           	 | float 	 |
| acquisitionTime 	 | acquisitiontime 	 | 4           	 | long  	 |

- The position and orientation objects aren't ordered but arrays **ARE**, meaning:
    ```json lines
    {x: Number, y: Number, z: Number}
    ``` 
  and
    ```json lines
   {z: Number, x: Number, y: Number}
    ```
  are two equal vectors, **but** `[1, 2, 3]` and `[3, 2, 1]` **AREN'T** equal.

# JSON generation
Currently, there isn't an automatic way to generate the JSON file. 

We plan to build additional tools to help automate the generation of the JSON file and JSON AprilTag scheme:
1. A website to generate them based on user entered paraments.
2. Serializer and deserializer for the Limelight AprilTag scheme to help integrate with a Limelight camera.
3. Serializer and deserializer for custom Advantage Scope fields.
