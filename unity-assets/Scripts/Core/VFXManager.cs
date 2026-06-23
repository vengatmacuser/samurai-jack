using UnityEngine;

namespace ChronoBlade.Core
{
    /// <summary>
    /// Manages combat feedback effects, leveraging the ObjectPool to instantiate visuals without lag.
    /// </summary>
    public class VFXManager : MonoBehaviour
    {
        public static VFXManager Instance { get; private set; }

        [Header("Pool Keys")]
        [SerializeField] private string hitVfxKey = "HitSlashVFX";
        [SerializeField] private string parryVfxKey = "ParryShockwaveVFX";

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
        /// Spawns a parry shockwave visual effect at a given coordinate.
        /// </summary>
        public void SpawnParryVFX(Vector3 position)
        {
            if (ObjectPool.Instance != null)
            {
                ObjectPool.Instance.SpawnFromPool(parryVfxKey, position, Quaternion.identity);
            }
        }

        /// <summary>
        /// Spawns a sword slash hit impact visual effect at a given coordinate.
        /// </summary>
        public void SpawnHitVFX(Vector3 position, Quaternion rotation)
        {
            if (ObjectPool.Instance != null)
            {
                ObjectPool.Instance.SpawnFromPool(hitVfxKey, position, rotation);
            }
        }
    }
}
