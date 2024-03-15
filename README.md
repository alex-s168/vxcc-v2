# VXCC
Compiler backend

This repository contains:
- common codegen utils
- x86 (and amd64) codegen
- [ua16](https://github.com/alex-s168/ua16) codegen and assembler
- VXCC-IR frontend

## Why
Really Hackable compiler backend with support for weird integer sizes and weird vector sizes.

## IR
VXCC-IR is a simple IR meant to be easy to pprocess.

Example code:
```
type int = :int w:16

export fn _start
    %counter = int 0
:loop
    %counter = int [add, %counter, int 1]
    [brl, :loop, %counter, int 100]
    
    ~ %counter
end
```
Example 2 (for x86):
```
type int = :int w:32
type sint = :int w:16
type ptr = :int w:32
type u8 = :int w:8

data putc_char 0

fn putc
    %charIn'al ? u8

    %char @mem u8 ::putc_char
    %char = %charIn
    ~ %char
    ~ %charIn
    %'eax = int 4
    %'ebx = int 1
    %'ecx = ptr [addr, ::putc_char]
    %'edx = int 1
    ! int 0x80
end

export fn _start
    %counter = sint 0
:loop
    %'al = int 65
    [call, ::putc]
    %counter = int [add, %counter, sint 1]
    [brl, :loop, %counter, sint 100]
    
    ~ %counter
    %'eax = int 1
    %'ebx = int 0
    ! int 0x80
end
```

## Status
This backend (the IR, the x86 codegen and the ua16 codegen) are far from finished.
A lot of IR programs won't even compile under specific circumstances
because not everything is implemented yet.

## Optimization
Since the IR frontend currently does not generate a proper AST,
there is no high-level optimization or good register allocator.
This will hopefully change in the future.

## Frontends for the IR
- None currently