using System.Collections.Generic;
using UnityEngine;
using UnityEngine.AddressableAssets;
using UnityEngine.ResourceManagement.AsyncOperations;
using UnityEngine.ResourceManagement.ResourceProviders;

namespace ChronoBlade.Core
{
    /// <summary>
    /// Manages dynamic loading and unloading of assets using Unity Addressables.
    /// Essential for keeping heap and VRAM usage low on mid-range Android devices.
    /// </summary>
    public class AssetLoadingManager : MonoBehaviour
    {
        public static AssetLoadingManager Instance { get; private set; }

        // Tracks loaded assets and their operation handles for safe disposal
        private readonly Dictionary<string, AsyncOperationHandle> loadedAssetHandles = new Dictionary<string, AsyncOperationHandle>();
        private SceneInstance currentLoadedSceneInstance;
        private string activeSceneAddress;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        /// <summary>
        /// Asynchronously loads a biome or level scene and unloads the previous one.
        /// </summary>
        public void LoadLevelScene(string sceneAddress, System.Action onCompleteCallback = null)
        {
            // Unload active scene first if it exists
            if (!string.IsNullOrEmpty(activeSceneAddress))
            {
                UnloadLevelScene(() => LoadNewScene(sceneAddress, onCompleteCallback));
            }
            else
            {
                LoadNewScene(sceneAddress, onCompleteCallback);
            }
        }

        private void LoadNewScene(string sceneAddress, System.Action onCompleteCallback)
        {
            activeSceneAddress = sceneAddress;
            Addressables.LoadSceneAsync(sceneAddress, UnityEngine.SceneManagement.LoadSceneMode.Single).Completed += handle =>
            {
                if (handle.Status == AsyncOperationStatus.Succeeded)
                {
                    currentLoadedSceneInstance = handle.Result;
                    onCompleteCallback?.Invoke();
                }
                else
                {
                    Debug.LogError($"[AssetLoadingManager] Failed to load scene: {sceneAddress}");
                }
            };
        }

        private void UnloadLevelScene(System.Action onUnloadComplete)
        {
            if (string.IsNullOrEmpty(activeSceneAddress)) return;

            Addressables.UnloadSceneAsync(currentLoadedSceneInstance).Completed += handle =>
            {
                activeSceneAddress = null;
                // Force a garbage collection sweep after major scene unloads to release memory immediately
                System.GC.Collect();
                Resources.UnloadUnusedAssets();
                onUnloadComplete?.Invoke();
            };
        }

        /// <summary>
        /// Loads a prefab asset (e.g., enemy, skin) dynamically, caching it to prevent double loads.
        /// </summary>
        public void LoadPrefab(string assetAddress, System.Action<GameObject> onComplete)
        {
            if (loadedAssetHandles.ContainsKey(assetAddress))
            {
                var cachedHandle = loadedAssetHandles[assetAddress];
                if (cachedHandle.IsDone && cachedHandle.Status == AsyncOperationStatus.Succeeded)
                {
                    onComplete?.Invoke(cachedHandle.Result as GameObject);
                    return;
                }
            }

            AsyncOperationHandle<GameObject> loadHandle = Addressables.LoadAssetAsync<GameObject>(assetAddress);
            loadedAssetHandles[assetAddress] = loadHandle;

            loadHandle.Completed += handle =>
            {
                if (handle.Status == AsyncOperationStatus.Succeeded)
                {
                    onComplete?.Invoke(handle.Result);
                }
                else
                {
                    Debug.LogError($"[AssetLoadingManager] Failed to load asset address: {assetAddress}");
                    loadedAssetHandles.Remove(assetAddress);
                }
            };
        }

        /// <summary>
        /// Releases all loaded assets from memory, freeing memory allocation handles.
        /// </summary>
        public void ReleaseAllAssets()
        {
            foreach (var key in loadedAssetHandles.Keys)
            {
                Addressables.Release(loadedAssetHandles[key]);
            }
            loadedAssetHandles.Clear();
            System.GC.Collect();
            Resources.UnloadUnusedAssets();
        }
    }
}
