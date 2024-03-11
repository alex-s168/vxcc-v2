package vxcc.cg

import vxcc.vxcc.x86.Owner

interface Optimal<T: Env<T>> {
    /** overall fastest boolean type */
    val boolFast: Owner.Flags

    /** overall smallest boolean type */
    val boolSmall: Owner.Flags

    /** fastest boolean type if speed opt, otherwise smallest boolean type */
    val bool: Owner.Flags

    /** overall fastest int type */
    val intFast: Owner.Flags

    /** overall smallest int type */
    val intSmall: Owner.Flags

    /** fastest int type if speed opt, otherwise smallest int type */
    val int: Owner.Flags
}