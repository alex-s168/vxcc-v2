# VXCC Binary Format
Version 1

## Format
(All numbers are little endian)

- Magic: `VXBIN` encoded in ASCII
- Version: 2 byte unsigned integer
- Target String: length-prefixed (2 byte unsigned integer) ASCII string
- Flags: 1 byte
- - mask `0b10000000`: linking finished?
- - mask `0b01000000`: position independent?
- if not position independent:
- - Origin: 8 byte unsigned long
- Section Header length: 2 byte unsigned integer that specifies the amount of sections
- Section Header: for each section:
- - Section name: length-prefixed (2 byte unsigned integer) ASCII string
- - Section offset (relative to end of section header): 4 byte unsigned integer
- - Section length: 4 byte unsigned integer
- - Symbols length: 2 byte unsigned integer that specifies the amount of symbols in the section
- - for each symbol in section:
- - - Name: length-prefixed (2 byte unsigned integer) ASCII string
- - - Position (relative to start of section): 4 byte unsigned integer
- - Unresolved References length: 2 byte unsigned integer that specifies the amount of unresolved references in the section
- - for each Unresolved Reference in section:
- - - Symbol Name: length-prefixed (2 byte unsigned integer) ASCII string
- - - Position (relative to start of section): 4 byte unsigned integer
- - - Kind: 1 byte unsigned integer; depends on target
- Sections

## Sections
All section names that start with `?` can be stripped away without affecting the functionality of the program.

|      |                                                                                                              |
|------|--------------------------------------------------------------------------------------------------------------|
| code | The main executable code of the binary; will be placed at optional origin                                    |
| data | All data of the binary. Data in this section should only be accessed via references; will be placed anywhere |