# Home Ambient Gallery

Media viewer / slideshow app that adjusts Bluetooth lighting to the image. For desktop.

* Select albums to view images, gifs and videos from.
* Sort by name, size, modification date, or shuffle randomly.
* Load tags for media file names from a CSV file, select or exclude tags to filter media.
* Toggle option to match the background and compatible Bluetooth lights to shown image.
* Or choose Bluetooth lights color manually.
* Start / stop slideshow, select interval between slides.

### Controls

Click shown image:

* Short or left click: show next media item.
* Long or right click: show previous item.

Click tag:

* Left click: select / clear selection.
If any tags are selected, only media with file names matching all of them will be shown.
* Right click: exclude / clear selection. 
Media with file names matching any of the excluded tags will not be shown.

Keyboard controls:

| Key   | Action                                |
|-------|---------------------------------------|
| Left  | Show previous media item              |
| Right | Show next media item                  |
| Up    | Pull up bottom sheet with view options |
| Down  | Close bottom sheet                 |
| F     | Enter / exit fullscreen               |
| F11   | Enter / exit fullscreen               |
| Esc   | Exit fullscreen                       |

## Build & run

Get the source code:

```
git clone https://github.com/alex-vt/HomeAmbientGallery
cd HomeAmbientGallery
```

### Desktop Linux

Requirements: Java

#### Run on desktop:

```
./gradlew run
```

#### Build executable:

```
./gradlew createDistributable
```

Folder with the app will be `build/compose/binaries/main/app/HomeAmbientGallery`

To run the app, in the app folder execute `bin/HomeAmbientGallery`

## Initial setup & settings

When started for the first time, the app views images from the downloads folder by default.

### Album folders

Edit the albums in settings (button appears above the bottom sheet when it's expanded).
Full paths, one per line. Media from non-hidden subfolders will be shown as well.

### Tags

Put the full path of a CSV file with tags in the settings to enable including and excluding 
viewed media by tags. The CSV file should have a header and 2 fields: 
file name, list of tags for the file.

Example of a CSV row in the file: `Sunrise,"nature,sun,sky"`

When there are tags available, options for showing and selecting them will show on the bottom sheet.

### Bluetooth lights

Put Bluetooth lights device MAC addresses in the settings to enable controlling them via app, 
one per line. Example of a MAC address: `01:23:45:AB:CD:EF`.

Generic Bluetooth Low Energy lights that accept characteristic `0x0009` 
value `56[red][green][blue]00f0aa` are supported. 
Red, green and blue are hexadecimal values of corresponding color component brightness, 
from `00` to `ff`. With all color component values `00`, light can be considered off.

Supported lights can be tested with the `gatttool` Linux command.
For a light with the example MAC address `01:23:45:AB:CD:EF` the commands for colors will be:

| Color     | `gatttool` example command                                                   |
|-----------|------------------------------------------------------------------------------|
| Light off | `gatttool -b 01:23:45:AB:CD:EF --char-write-req -a 0x0009 -n 5600000000f0aa` |
| Red       | `gatttool -b 01:23:45:AB:CD:EF --char-write-req -a 0x0009 -n 56ff000000f0aa` |
| Green     | `gatttool -b 01:23:45:AB:CD:EF --char-write-req -a 0x0009 -n 5600ff0000f0aa` |
| Blue      | `gatttool -b 01:23:45:AB:CD:EF --char-write-req -a 0x0009 -n 560000ff00f0aa` |
| White     | `gatttool -b 01:23:45:AB:CD:EF --char-write-req -a 0x0009 -n 56ffffff00f0aa` |

When Bluetooth lights are added in settings, the relevant options will show on the bottom sheet.


## Development

Tech stack: Kotlin, Compose Multiplatform

Bluetooth control: `gatttool` command line util on desktop Linux

Build system: Gradle

## License

[MIT](LICENSE) license.
