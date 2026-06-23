using UnityEngine;
using ChronoBlade.Input;

namespace ChronoBlade.Combat
{
    [RequireComponent(typeof(CharacterController))]
    [RequireComponent(typeof(Animator))]
    public class PlayerController : MonoBehaviour
    {
        [Header("Movement Settings")]
        public float moveSpeed = 6f;
        public float jumpForce = 8f;
        public float gravity = 20f;
        public float rotationSpeed = 15f;

        [Header("Combat Settings")]
        public float attackRange = 2.5f;
        public float parryWindowDuration = 0.2f; // 12 frames at 60Hz
        public float kiaiMaxEnergy = 100f;
        public LayerMask enemyLayer;

        [Header("Stats")]
        [Range(0f, 100f)] public float currentHealth = 100f;
        public float currentSwordEnergy = 0f;

        // References
        public CharacterController CharacterController { get; private set; }
        public Animator Animator { get; private set; }
        public Camera MainCamera { get; private set; }

        // State Machine States
        public IPlayerState CurrentState { get; private set; }
        public IdleState IdleState { get; private set; }
        public RunState RunState { get; private set; }
        public AttackState AttackState { get; private set; }
        public ParryState ParryState { get; private set; }
        public DodgeState DodgeState { get; private set; }

        // Movement Physics helper variables
        [HideInInspector] public Vector3 moveVelocity;
        [HideInInspector] public float targetRotationY;
        public bool IsGrounded => CharacterController.isGrounded;

        // Auto-Targeting details
        [HideInInspector] public Transform lockTarget;

        private void Awake()
        {
            CharacterController = GetComponent<CharacterController>();
            Animator = GetComponent<Animator>();
            MainCamera = Camera.main;

            // Instantiate and cache concrete states to avoid GC allocations during runtime state changes
            IdleState = new IdleState();
            RunState = new RunState();
            AttackState = new AttackState();
            ParryState = new ParryState();
            DodgeState = new DodgeState();
        }

        private void Start()
        {
            // Initialize with Idle State
            TransitionToState(IdleState);
        }

        private void Update()
        {
            CurrentState?.Update(this);
            ApplyMovementPhysics();
        }

        private void FixedUpdate()
        {
            CurrentState?.FixedUpdate(this);
        }

        public void TransitionToState(IPlayerState newState)
        {
            CurrentState?.Exit(this);
            CurrentState = newState;
            CurrentState?.Enter(this);
        }

        private void ApplyMovementPhysics()
        {
            // Apply gravity manually for custom character controller handling
            if (IsGrounded && moveVelocity.y < 0)
            {
                moveVelocity.y = -2f; // Keep stuck to the ground
            }
            else
            {
                moveVelocity.y -= gravity * Time.deltaTime;
            }

            // Move the character controller
            CharacterController.Move(moveVelocity * Time.deltaTime);

            // Smoothly rotate character toward target direction
            if (new Vector2(moveVelocity.x, moveVelocity.z).magnitude > 0.1f)
            {
                float smoothRotation = Mathf.LerpAngle(transform.eulerAngles.y, targetRotationY, rotationSpeed * Time.deltaTime);
                transform.rotation = Quaternion.Euler(0f, smoothRotation, 0f);
            }
        }

        /// <summary>
        /// Performs sphere-cast auto-targeting in the direction of movement or towards the nearest enemy.
        /// </summary>
        public void FindAutoTarget()
        {
            lockTarget = null;
            Vector3 center = transform.position + transform.forward * (attackRange * 0.5f);
            Collider[] hitColliders = Physics.OverlapSphere(center, attackRange, enemyLayer);

            float closestDistance = Mathf.Infinity;
            foreach (var col in hitColliders)
            {
                float distance = Vector3.Distance(transform.position, col.transform.position);
                if (distance < closestDistance)
                {
                    closestDistance = distance;
                    lockTarget = col.transform;
                }
            }
        }

        /// <summary>
        /// Applies damage to Jack. Block reduces damage, Parry deflects it.
        /// </summary>
        public void TakeDamage(float damage, Transform source)
        {
            if (CurrentState == ParryState)
            {
                // Check if attacker is in front of Jack for valid parry
                Vector3 toAttacker = (source.position - transform.position).normalized;
                float dot = Vector3.Dot(transform.forward, toAttacker);
                if (dot > 0.5f)
                {
                    OnSuccessfulParry(source);
                    return;
                }
            }

            // Apply full damage
            currentHealth = Mathf.Max(currentHealth - damage, 0f);
            Animator.SetTrigger("Hurt");
            
            if (currentHealth <= 0f)
            {
                Animator.SetTrigger("Die");
                // Notify GameManager of death
            }
        }

        private void OnSuccessfulParry(Transform attacker)
        {
            // Parry feedback: Time Slow, Audio Event, Energy Gain
            currentSwordEnergy = Mathf.Min(currentSwordEnergy + 20f, kiaiMaxEnergy);
            
            // Execute parry counters/staggers on the attacker if applicable
            var enemy = attacker.GetComponent<IDamageable>();
            enemy?.TakeStaggerDamage(50f); // Deplete enemy posture

            Animator.SetTrigger("ParrySuccess");
            
            // Visual shockwave feedback trigger
            VFXManager.Instance?.SpawnParryVFX(transform.position + Vector3.up * 1f);
        }
    }

    public interface IDamageable
    {
        void TakeDamage(float amount);
        void TakeStaggerDamage(float postureAmount);
    }
}
