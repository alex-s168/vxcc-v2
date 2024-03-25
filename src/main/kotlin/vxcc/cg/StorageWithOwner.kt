package vxcc.cg

interface StorageWithOwner<E: CGEnv<E>>: Storage<E> {
    val owner: Owner<E>
}