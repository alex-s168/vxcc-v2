# VXCC Binary Format
Version 0

## Format
(All numbers are little endian)

- Magic: `VXBIN` encoded in ASCII
- Version: 2 byte unsigned integer
- Target String: length-prefixed (2 byte unsigned integer) ASCII string
- Flags: 1 byte
- - mask `0b10000000`: linking finished?
- - mask `0b01000000`: position independent?
- Section Header length: 2 byte unsigned integer that specifies the amount of sections
- Section Header: for each section:
- - Section name: length-prefixed (2 byte unsigned integer) ASCII string
- - Section offset (relative to end of section header): 4 byte unsigned integer
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