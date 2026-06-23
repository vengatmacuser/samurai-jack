using System;
using UnityEngine;
using ChronoBlade.Combat;

namespace ChronoBlade.Core
{
    public enum GameState
    {
        SPLASH,
        MAIN_MENU,
        STAGE_SELECT,
        TRAVEL_TRANSITION,
        INTRO_CUTSCENE,
        GAMEPLAY,
        OUTRO_CUTSCENE,
        GAME_OVER,
        GAME_COMPLETE
    }

    /// <summary>
    /// Master orchestration manager handling the overall game loops, scene loads, and active game states.
    /// </summary>
    public class GameManager : MonoBehaviour
    {
        public static GameManager Instance { get; private set; }

        [Header("Global Settings")]
        public GameState currentGameState = GameState.SPLASH;
        public int unlockedStageCount = 13;
        public int activeStageIndex = 0;

        // Player instance references
        private PlayerController playerController;

        // Event notifications for UI triggers
        public static event Action<GameState> OnGameStateChanged;
        public static event Action<int> OnCoinsCollectedChanged;

        private int coinsCollected = 0;
        public int CoinsCollected
        {
            get => coinsCollected;
            set
            {
                coinsCollected = value;
                OnCoinsCollectedChanged?.Invoke(coinsCollected);
            }
        }

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

        private void Start()
        {
            SetGameState(GameState.SPLASH);
            // In a full Unity flow, we would trigger transition to MAIN_MENU after a splash delay
        }

        public void SetGameState(GameState newState)
        {
            currentGameState = newState;
            OnGameStateChanged?.Invoke(currentGameState);

            switch (currentGameState)
            {
                case GameState.MAIN_MENU:
                    Time.timeScale = 1.0f;
                    break;
                case GameState.GAMEPLAY:
                    Time.timeScale = 1.0f;
                    // Reference dynamic player instance inside scene
                    playerController = FindFirstObjectByType<PlayerController>();
                    break;
                case GameState.GAME_OVER:
                    Time.timeScale = 0.0f; // Freeze game actions on death screen
                    break;
                case GameState.GAME_COMPLETE:
                    Time.timeScale = 0.0f;
                    break;
            }
        }

        /// <summary>
        /// Transitions levels and loads assets dynamically.
        /// </summary>
        public void LoadStage(int stageIndex)
        {
            activeStageIndex = stageIndex;
            SetGameState(GameState.TRAVEL_TRANSITION);

            string addressKey = $"Stage_{activeStageIndex + 1}_Scene";

            AssetLoadingManager.Instance.LoadLevelScene(addressKey, () =>
            {
                // After scene loaded, trigger intro dialogue sequence
                SetGameState(GameState.INTRO_CUTSCENE);
            });
        }

        public void CompleteStage()
        {
            if (activeStageIndex >= 12)
            {
                SetGameState(GameState.GAME_COMPLETE);
            }
            else
            {
                unlockedStageCount = Mathf.Max(unlockedStageCount, activeStageIndex + 2);
                SetGameState(GameState.OUTRO_CUTSCENE);
            }
        }

        public void ReloadCurrentStage()
        {
            LoadStage(activeStageIndex);
        }

        public void ReturnToMainMenu()
        {
            AssetLoadingManager.Instance.ReleaseAllAssets();
            Addressables.LoadSceneAsync("MainMenuScene").Completed += handle =>
            {
                SetGameState(GameState.MAIN_MENU);
            };
        }
    }
}
