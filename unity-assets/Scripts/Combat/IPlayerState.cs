namespace ChronoBlade.Combat
{
    /// <summary>
    /// Base interface for all states in the character Finite State Machine.
    /// Provides hook points for entering, ticking, physics ticking, and exiting states.
    /// </summary>
    public interface IPlayerState
    {
        /// <summary>
        /// Executed when the player controller enters this state.
        /// </summary>
        void Enter(PlayerController player);

        /// <summary>
        /// Executed every frame during Update.
        /// </summary>
        void Update(PlayerController player);

        /// <summary>
        /// Executed every frame during FixedUpdate for physics operations.
        /// </summary>
        void FixedUpdate(PlayerController player);

        /// <summary>
        /// Executed when transitioning away from this state.
        /// </summary>
        void Exit(PlayerController player);
    }
}
