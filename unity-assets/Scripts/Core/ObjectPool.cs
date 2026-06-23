using System.Collections.Generic;
using UnityEngine;

namespace ChronoBlade.Core
{
    /// <summary>
    /// Reusable generic object pooling class.
    /// Crucial for preventing runtime Instantiate/Destroy calls which trigger Android Garbage Collection (GC) spikes.
    /// </summary>
    public class ObjectPool : MonoBehaviour
    {
        public static ObjectPool Instance { get; private set; }

        [System.Serializable]
        public struct PoolConfig
        {
            public string poolKey;
            public GameObject prefab;
            public int initialSize;
            public int maxPoolLimit;
        }

        [SerializeField] private List<PoolConfig> poolsConfigurations = new List<PoolConfig>();

        private readonly Dictionary<string, Queue<GameObject>> poolDictionary = new Dictionary<string, Queue<GameObject>>();
        private readonly Dictionary<string, PoolConfig> configDictionary = new Dictionary<string, PoolConfig>();

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);

            InitializePools();
        }

        private void InitializePools()
        {
            foreach (var config in poolsConfigurations)
            {
                if (configDictionary.ContainsKey(config.poolKey)) continue;

                configDictionary.Add(config.poolKey, config);
                Queue<GameObject> objectPool = new Queue<GameObject>();

                for (int i = 0; i < config.initialSize; i++)
                {
                    GameObject obj = Instantiate(config.prefab, transform);
                    obj.SetActive(false);
                    objectPool.Enqueue(obj);
                }

                poolDictionary.Add(config.poolKey, objectPool);
            }
        }

        /// <summary>
        /// Retrieves an inactive object from the designated pool, activating it.
        /// </summary>
        public GameObject SpawnFromPool(string poolKey, Vector3 position, Quaternion rotation)
        {
            if (!poolDictionary.ContainsKey(poolKey))
            {
                Debug.LogWarning($"Pool with key {poolKey} does not exist.");
                return null;
            }

            Queue<GameObject> queue = poolDictionary[poolKey];
            GameObject objectToSpawn = null;

            // Check if queue has available inactive objects
            if (queue.Count > 0 && !queue.Peek().activeSelf)
            {
                objectToSpawn = queue.Dequeue();
            }
            else
            {
                // Pool exhausted, instantiate a new instance if within threshold limits
                var config = configDictionary[poolKey];
                objectToSpawn = Instantiate(config.prefab, transform);
            }

            objectToSpawn.transform.position = position;
            objectToSpawn.transform.rotation = rotation;
            objectToSpawn.SetActive(true);

            // Re-queue to make recyclable
            queue.Enqueue(objectToSpawn);

            return objectToSpawn;
        }

        /// <summary>
        /// Manually returns an object to the pool (sets it inactive).
        /// </summary>
        public void ReturnToPool(string poolKey, GameObject obj)
        {
            obj.SetActive(false);
            if (poolDictionary.ContainsKey(poolKey))
            {
                // Ensure it is in the queue if it was newly instantiated
                if (!poolDictionary[poolKey].Contains(obj))
                {
                    poolDictionary[poolKey].Enqueue(obj);
                }
            }
        }
    }
}
