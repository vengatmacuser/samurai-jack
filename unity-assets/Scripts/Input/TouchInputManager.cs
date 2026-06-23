using UnityEngine;
using UnityEngine.Events;
using UnityEngine.InputSystem;

namespace ChronoBlade.Input
{
    /// <summary>
    /// Captures touch gestures (taps, swipes, drags) and translates them to game actions.
    /// Optimized for mobile performance, avoiding runtime memory allocations.
    /// </summary>
    [DefaultExecutionOrder(-100)]
    public class TouchInputManager : MonoBehaviour
    {
        public static TouchInputManager Instance { get; private set; }

        [Header("Ergonomic Thresholds")]
        [Tooltip("Minimum drag distance in pixels to register a swipe gesture.")]
        [SerializeField] private float swipeMinDistance = 60f;
        [Tooltip("Maximum duration in seconds to count a press as a quick tap.")]
        [SerializeField] private float tapMaxDuration = 0.25f;

        [Header("Virtual Joystick Zones")]
        [Tooltip("Screen width fraction defining the joystick quadrant (e.g., 0.35f = left 35% of the screen).")]
        [Range(0.1f, 0.5f)]
        [SerializeField] private float joystickZoneWidthFraction = 0.35f;

        // Input Actions (Unity 6 Input System)
        private InputAction touchPositionAction;
        private InputAction touchContactAction;

        // Tracked touch state variables
        private Vector2 touchStartPos;
        private float touchStartTime;
        private bool isTouchActive;
        private bool touchStartedInJoystickZone;

        // Output states accessed by the PlayerController
        public Vector2 MovementInput { get; private set; }
        public bool AttackTapped { get; private set; }
        public bool JumpSwiped { get; private set; }
        public bool BlockSwiped { get; private set; }
        public int DodgeSwipeDirection { get; private set; } // -1 = Left, 1 = Right, 0 = None

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);

            InitializeInputActions();
        }

        private void OnEnable()
        {
            touchPositionAction?.Enable();
            touchContactAction?.Enable();
        }

        private void OnDisable()
        {
            touchPositionAction?.Disable();
            touchContactAction?.Disable();
        }

        private void InitializeInputActions()
        {
            // Set up actions programmatically to avoid external asset dependency issues
            var map = new InputActionMap("TouchControls");

            touchPositionAction = map.AddAction("TouchPosition", binding: "<Touchscreen>/primaryTouch/position");
            touchContactAction = map.AddAction("TouchContact", binding: "<Touchscreen>/primaryTouch/press");

            touchContactAction.started += OnTouchStarted;
            touchContactAction.canceled += OnTouchEnded;
        }

        private void Update()
        {
            if (isTouchActive && touchStartedInJoystickZone)
            {
                ProcessJoystickMovement();
            }
            else
            {
                MovementInput = Vector2.zero;
            }
        }

        private void LateUpdate()
        {
            // Clear single-frame action flags at the end of the frame
            AttackTapped = false;
            JumpSwiped = false;
            BlockSwiped = false;
            DodgeSwipeDirection = 0;
        }

        private void OnTouchStarted(InputAction.CallbackContext context)
        {
            Vector2 currentPosition = touchPositionAction.ReadValue<Vector2>();
            touchStartPos = currentPosition;
            touchStartTime = Time.time;
            isTouchActive = true;

            // Ergonomics: Check if touch starts in left portion of the screen (Virtual Joystick Zone)
            float joystickThresholdWidth = Screen.width * joystickZoneWidthFraction;
            touchStartedInJoystickZone = currentPosition.x <= joystickThresholdWidth;
        }

        private void OnTouchEnded(InputAction.CallbackContext context)
        {
            isTouchActive = false;
            
            if (touchStartedInJoystickZone)
            {
                MovementInput = Vector2.zero;
                return;
            }

            Vector2 endPosition = touchPositionAction.ReadValue<Vector2>();
            Vector2 delta = endPosition - touchStartPos;
            float duration = Time.time - touchStartTime;

            // Classify gesture: Swipe or Tap
            if (delta.magnitude >= swipeMinDistance)
            {
                ProcessSwipeGesture(delta);
            }
            else if (duration <= tapMaxDuration)
            {
                AttackTapped = true;
            }
        }

        private void ProcessJoystickMovement()
        {
            Vector2 currentPosition = touchPositionAction.ReadValue<Vector2>();
            Vector2 dragVector = currentPosition - touchStartPos;

            // Normalize dragVector against a nominal max drag distance (e.g., 100 pixels)
            float maxJoystickDistance = 100f;
            MovementInput = Vector2.ClampMagnitude(dragVector / maxJoystickDistance, 1.0f);
        }

        private void ProcessSwipeGesture(Vector2 swipeVector)
        {
            // Determine primary direction of swipe
            if (Mathf.Abs(swipeVector.y) > Mathf.Abs(swipeVector.x))
            {
                // Vertical Swipe
                if (swipeVector.y > 0)
                {
                    JumpSwiped = true;
                }
                else
                {
                    BlockSwiped = true;
                }
            }
            else
            {
                // Horizontal Swipe (Dodge)
                DodgeSwipeDirection = swipeVector.x > 0 ? 1 : -1;
            }
        }
    }
}
