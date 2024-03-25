# Branch Fusion
AMD CPUs since (including) Bulldozer
can merge specific comparisons
with conditional branch instruction at runtime
to make them pass through the CPU as one instruction.

This only works with the `cmp` and `test` instructions
and any conditional branch instruction.

# Macro Fusion
Intel CPUs since (including) Sandy Bridge
can merge instructions that set conditional flags
with specific conditional branch instructions at runtime
to make them pass through the CPU as one instruction.

This works for the following combinations of instructions:

|                           | branch                                                          |
|---------------------------|-----------------------------------------------------------------|
| `test`                    | any                                                             |
| `and`                     | any                                                             |
| `inc`, `dec`              | `je`, `jne`, `jl`, `jle`, `jg`, `jge`                           |
| `cmp`                     | `je`, `jne`, `jl`, `jle`, `jg`, `jge`, `ja`, `jae`, `jb`, `jbe` |
| any other ALU instruction | `je`, `jne`, `jl`, `jle`, `jg`, `jge`, `ja`, `jae`, `jb`, `jbe` |
