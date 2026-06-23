using UnityEngine;
using ChronoBlade.Input;

namespace ChronoBlade.Combat
{
    // ==========================================
    // IDLE STATE
    // ==========================================
    public class IdleState : IPlayerState
    {
        public void Enter(PlayerController player)
        {
            player.Animator.SetFloat("Speed", 0f);
            player.moveVelocity.x = 0f;
            player.moveVelocity.z = 0f;
        }

        public void Update(PlayerController player)
        {
            var input = TouchInputManager.Instance;
            if (input == null) return;

            // Check transitions
            if (input.AttackTapped)
            {
                player.TransitionToState(player.AttackState);
                return;
            }
            if (input.BlockSwiped)
            {
                player.TransitionToState(player.ParryState);
                return;
            }
            if (input.DodgeSwipeDirection != 0)
            {
                player.TransitionToState(player.DodgeState);
                return;
            }
            if (input.JumpSwiped && player.IsGrounded)
            {
                player.moveVelocity.y = player.jumpForce;
                player.Animator.SetTrigger("Jump");
            }
            if (input.MovementInput.magnitude > 0.15f)
            {
                player.TransitionToState(player.RunState);
            }
        }

        public void FixedUpdate(PlayerController player) { }
        public void Exit(PlayerController player) { }
    }

    // ==========================================
    // RUN STATE
    // ==========================================
    public class RunState : IPlayerState
    {
        public void Enter(PlayerController player) { }

        public void Update(PlayerController player)
        {
            var input = TouchInputManager.Instance;
            if (input == null) return;

            // Check combat interrupts
            if (input.AttackTapped)
            {
                player.TransitionToState(player.AttackState);
                return;
            }
            if (input.BlockSwiped)
            {
                player.TransitionToState(player.ParryState);
                return;
            }
            if (input.DodgeSwipeDirection != 0)
            {
                player.TransitionToState(player.DodgeState);
                return;
            }
            if (input.JumpSwiped && player.IsGrounded)
            {
                player.moveVelocity.y = player.jumpForce;
                player.Animator.SetTrigger("Jump");
            }

            Vector2 moveInput = input.MovementInput;
            if (moveInput.magnitude < 0.15f)
            {
                player.TransitionToState(player.IdleState);
                return;
            }

            // Calculate camera-relative movement vector
            Vector3 camForward = player.MainCamera.transform.forward;
            Vector3 camRight = player.MainCamera.transform.right;
            camForward.y = 0;
            camRight.y = 0;
            camForward.Normalize();
            camRight.Normalize();

            Vector3 moveDirection = camForward * moveInput.y + camRight * moveInput.x;

            // Apply horizontal speed
            player.moveVelocity.x = moveDirection.x * player.moveSpeed;
            player.moveVelocity.z = moveDirection.z * player.moveSpeed;

            // Calculate rotation target angle (in degrees)
            player.targetRotationY = Mathf.Atan2(moveDirection.x, moveDirection.z) * Mathf.Rad2Deg;

            // Update Animator speed variable
            player.Animator.SetFloat("Speed", moveInput.magnitude);
        }

        public void FixedUpdate(PlayerController player) { }
        public void Exit(PlayerController player) { }
    }

    // ==========================================
    // ATTACK STATE
    // ==========================================
    public class AttackState : IPlayerState
    {
        private float attackTimer;
        private const float attackDuration = 0.5f; // Matches standard slash animation duration

        public void Enter(PlayerController player)
        {
            attackTimer = attackDuration;
            player.Animator.SetTrigger("Attack");
            player.moveVelocity.x = 0f;
            player.moveVelocity.z = 0f;

            // Align player toward the nearest enemy in range
            player.FindAutoTarget();
            if (player.lockTarget != null)
            {
                Vector3 lookDir = (player.lockTarget.position - player.transform.position).normalized;
                lookDir.y = 0f;
                player.transform.rotation = Quaternion.LookRotation(lookDir);
            }
        }

        public void Update(PlayerController player)
        {
            attackTimer -= Time.deltaTime;
            if (attackTimer <= 0f)
            {
                player.TransitionToState(player.IdleState);
            }
        }

        public void FixedUpdate(PlayerController player) { }

        public void Exit(PlayerController player)
        {
            // Clean up any targeting parameters
            player.lockTarget = null;
        }
    }

    // ==========================================
    // PARRY & BLOCK STATE
    // ==========================================
    public class ParryState : IPlayerState
    {
        private float parryTimer;

        public void Enter(PlayerController player)
        {
            parryTimer = player.parryWindowDuration;
            player.Animator.SetBool("IsBlocking", true);
            player.moveVelocity.x = 0f;
            player.moveVelocity.z = 0f;
        }

        public void Update(PlayerController player)
        {
            if (parryTimer > 0f)
            {
                parryTimer -= Time.deltaTime;
                // Perfect Parry window is active while parryTimer > 0
            }

            // Exits when swipe hold ends (simulated by timeout or input release)
            var input = TouchInputManager.Instance;
            if (input == null || !input.JumpSwiped) // Example condition to release block on other gestures
            {
                // In actual mobile production, block lasts until release of finger or 1.0s timeout
            }
            
            // Auto release block after 0.8s to keep mobile pace active
            if (parryTimer < -0.6f)
            {
                player.TransitionToState(player.IdleState);
            }
        }

        public void FixedUpdate(PlayerController player) { }

        public void Exit(PlayerController player)
        {
            player.Animator.SetBool("IsBlocking", false);
        }
    }

    // ==========================================
    // DODGE STATE (I-Frames)
    // ==========================================
    public class DodgeState : IPlayerState
    {
        private float dodgeTimer;
        private const float dodgeDuration = 0.4f;
        private Vector3 dodgeDirection;
        private const float dodgeSpeed = 12f;

        public void Enter(PlayerController player)
        {
            dodgeTimer = dodgeDuration;
            player.Animator.SetTrigger("Dodge");

            int swipeDir = TouchInputManager.Instance != null ? TouchInputManager.Instance.DodgeSwipeDirection : 1;
            
            // Calculate direction relative to camera orientation
            Vector3 camRight = player.MainCamera.transform.right;
            camRight.y = 0f;
            camRight.Normalize();

            dodgeDirection = camRight * swipeDir;
            
            // Rotate character to face the dodge direction
            if (dodgeDirection != Vector3.zero)
            {
                player.transform.rotation = Quaternion.LookRotation(dodgeDirection);
            }

            // Temporarily ignore collision layers (I-frames) or apply tag modifications
            player.gameObject.layer = LayerMask.NameToLayer("Ignore Raycast");
        }

        public void Update(PlayerController player)
        {
            dodgeTimer -= Time.deltaTime;

            // Apply linear dodge movement
            player.moveVelocity.x = dodgeDirection.x * dodgeSpeed;
            player.moveVelocity.z = dodgeDirection.z * dodgeSpeed;

            if (dodgeTimer <= 0f)
            {
                player.TransitionToState(player.IdleState);
            }
        }

        public void FixedUpdate(PlayerController player) { }

        public void Exit(PlayerController player)
        {
            // Restore default player layer
            player.gameObject.layer = LayerMask.NameToLayer("Player");
        }
    }
}
