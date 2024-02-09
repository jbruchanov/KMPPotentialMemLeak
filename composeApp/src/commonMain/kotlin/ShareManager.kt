interface ShareManager {

    suspend fun requestPhotos() : DeferredDataProvider
}