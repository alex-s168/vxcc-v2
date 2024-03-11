package vxcc.cg

interface StorageWithOwner<E: Env<E>>: Storage<E> {
    val owner: Owner<E>
}