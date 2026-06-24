package com.thigazhini_labs.samuraijack.stages

data class StageConfig(
    val stageNumber: Int,
    val title: String,
    val subtitle: String,
    val objective: String,
    val bgColor: FloatArray,      // R, G, B for GLES30.glClearColor
    val fogColor: FloatArray,     // R, G, B
    val fogDensity: Float,
    val groundColor: FloatArray,  // R, G, B
    val dialogs: List<String>,
    val enemyCount: Int,
    val bossType: String          // "None", "Aku", "BeetleBoss", "LavaGuardian", "HighlandWarrior", "Beast", "Ronin"
)

object Stages {
    val stagesList = listOf(
        StageConfig(
            stageNumber = 1,
            title = "Frosthollow Mine",
            subtitle = "The Frozen Tomb",
            objective = "Cross the cold abandoned mine deep in the northern mountains.",
            bgColor = floatArrayOf(0.05f, 0.08f, 0.15f),
            fogColor = floatArrayOf(0.12f, 0.11f, 0.1f),
            fogDensity = 0.052f,
            groundColor = floatArrayOf(0.2f, 0.25f, 0.3f),
            dialogs = listOf(
                "Aku: The freezing depths of Frosthollow Mine shall be your tomb, Samurai!",
                "Jack: My spirit burns warmer than any fire, Aku. I will traverse these frozen gates!",
                "Narrator: The wooden bridge is icy. Proceed with caution."
            ),
            enemyCount = 3,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 2,
            title = "The Young Prince",
            subtitle = "First Training",
            objective = "Complete training missions and learn combat mechanics.",
            bgColor = floatArrayOf(0.1f, 0.15f, 0.12f),
            fogColor = floatArrayOf(0.18f, 0.28f, 0.22f),
            fogDensity = 0.02f,
            groundColor = floatArrayOf(0.35f, 0.25f, 0.18f),
            dialogs = listOf(
                "Master: Calm your mind, young prince. Let your blade be an extension of your spirit.",
                "Jack: Yes, Master. I will master every discipline.",
                "Master: Swipe to dodge and strike when your enemy is off balance!"
            ),
            enemyCount = 4,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 3,
            title = "Journey Across the World",
            subtitle = "Across the Seas",
            objective = "Master different fighting styles and abilities.",
            bgColor = floatArrayOf(0.2f, 0.18f, 0.15f),
            fogColor = floatArrayOf(0.45f, 0.35f, 0.25f),
            fogDensity = 0.03f,
            groundColor = floatArrayOf(0.65f, 0.5f, 0.35f),
            dialogs = listOf(
                "Sailor: We sail through treacherous waters, boy. Keep your blade close.",
                "Viking: Show us your strength! Fight alongside us!",
                "Jack: Every land has lessons to teach, and every battle makes me stronger."
            ),
            enemyCount = 5,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 4,
            title = "The Sacred Sword",
            subtitle = "Summit of Light",
            objective = "Obtain the legendary sword and unlock special attacks.",
            bgColor = floatArrayOf(0.15f, 0.18f, 0.22f),
            fogColor = floatArrayOf(0.5f, 0.6f, 0.75f),
            fogDensity = 0.015f,
            groundColor = floatArrayOf(0.85f, 0.85f, 0.9f),
            dialogs = listOf(
                "Monk: Only the pure of heart can retrieve the sword from the sacred peak.",
                "Jack: I must reach the summit. My father's kingdom depends on it.",
                "Narrator: The blade shines with heavenly light. Special Slash unlocked!"
            ),
            enemyCount = 4,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 5,
            title = "Battle Against Evil",
            subtitle = "Face of Darkness",
            objective = "Fight the first major boss and attempt to stop the darkness.",
            bgColor = floatArrayOf(0.22f, 0.08f, 0.05f),
            fogColor = floatArrayOf(0.55f, 0.15f, 0.08f),
            fogDensity = 0.035f,
            groundColor = floatArrayOf(0.25f, 0.22f, 0.25f),
            dialogs = listOf(
                "Aku: You are a fool to challenge me, mortal!",
                "Jack: Your reign of terror ends here, Aku!",
                "Aku: Fool! You cannot escape my grasp!"
            ),
            enemyCount = 1,
            bossType = "Aku"
        ),
        StageConfig(
            stageNumber = 6,
            title = "Exiled Through Time",
            subtitle = "A Future Unveiled",
            objective = "Survive the time portal and arrive in the future.",
            bgColor = floatArrayOf(0.08f, 0.05f, 0.15f),
            fogColor = floatArrayOf(0.25f, 0.12f, 0.45f),
            fogDensity = 0.05f,
            groundColor = floatArrayOf(0.1f, 0.08f, 0.2f),
            dialogs = listOf(
                "Aku: Before the final blow was struck, I tore open a portal in time!",
                "Aku: And flung him into the future, where my evil is law!",
                "Jack: Where... where am I? What has he done?!"
            ),
            enemyCount = 5,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 7,
            title = "City of Machines",
            subtitle = "Drones of Metropolis",
            objective = "Explore a futuristic city ruled by robotic armies.",
            bgColor = floatArrayOf(0.05f, 0.08f, 0.08f),
            fogColor = floatArrayOf(0.08f, 0.22f, 0.12f),
            fogDensity = 0.045f,
            groundColor = floatArrayOf(0.12f, 0.12f, 0.15f),
            dialogs = listOf(
                "Rothchild: Good heavens! A samurai! We canine archaeologists require assistance.",
                "Robot Drone: Intruders detected. Terminate immediately.",
                "Jack: These mechanical beasts are ruthless... I must destroy them!"
            ),
            enemyCount = 6,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 8,
            title = "The Dog Archer Village",
            subtitle = "The Great Siege",
            objective = "Defend the village from invading enemies.",
            bgColor = floatArrayOf(0.05f, 0.05f, 0.08f),
            fogColor = floatArrayOf(0.12f, 0.12f, 0.22f),
            fogDensity = 0.035f,
            groundColor = floatArrayOf(0.18f, 0.18f, 0.22f),
            dialogs = listOf(
                "Rothchild: The beetle drones are launching another assault on our excavations!",
                "Jack: Stand back. My blade will shield your village.",
                "Beetle-Drone: Alert! Target Samurai identified. Deploying all units!"
            ),
            enemyCount = 8,
            bossType = "BeetleBoss"
        ),
        StageConfig(
            stageNumber = 9,
            title = "The Warrior from the Highlands",
            subtitle = "Rivalry of Iron",
            objective = "Meet a rival warrior and prove your strength.",
            bgColor = floatArrayOf(0.1f, 0.1f, 0.12f),
            fogColor = floatArrayOf(0.25f, 0.25f, 0.3f),
            fogDensity = 0.025f,
            groundColor = floatArrayOf(0.2f, 0.25f, 0.2f),
            dialogs = listOf(
                "Scotsman: Hey! Watch where you're walking, you dress-wearing, toothpick-wielding scamp!",
                "Jack: I seek no quarrel, traveler. Let me pass.",
                "Scotsman: Passed? Nobody passes the Scotsman without a proper fight!"
            ),
            enemyCount = 1,
            bossType = "HighlandWarrior"
        ),
        StageConfig(
            stageNumber = 10,
            title = "Escape from the Fortress",
            subtitle = "Fortress Breakout",
            objective = "Work together to break out of an enemy stronghold.",
            bgColor = floatArrayOf(0.08f, 0.08f, 0.1f),
            fogColor = floatArrayOf(0.15f, 0.18f, 0.25f),
            fogDensity = 0.03f,
            groundColor = floatArrayOf(0.14f, 0.14f, 0.18f),
            dialogs = listOf(
                "Scotsman: We're cuffed together, laddie! Let's bust these tin cans!",
                "Jack: Direct your strikes outward! I will cover your back.",
                "Scotsman: Now that's what I call teamwork! Ha-ha!"
            ),
            enemyCount = 6,
            bossType = "None"
        ),
        StageConfig(
            stageNumber = 11,
            title = "The Three-Eyed Beast",
            subtitle = "Toxic Dunes",
            objective = "Hunt and defeat a powerful monster in the wastelands.",
            bgColor = floatArrayOf(0.12f, 0.08f, 0.12f),
            fogColor = floatArrayOf(0.35f, 0.15f, 0.35f),
            fogDensity = 0.04f,
            groundColor = floatArrayOf(0.25f, 0.2f, 0.25f),
            dialogs = listOf(
                "Villager: A terrifying three-eyed titan roams the toxic dunes.",
                "Jack: Do not fear. I will hunt this shadow beast.",
                "Beast: Roaaaarrrrr!"
            ),
            enemyCount = 1,
            bossType = "Beast"
        ),
        StageConfig(
            stageNumber = 12,
            title = "The Lava Guardian",
            subtitle = "The Lava Core",
            objective = "Cross volcanic lands and defeat the Lava Guardian.",
            bgColor = floatArrayOf(0.2f, 0.05f, 0.02f),
            fogColor = floatArrayOf(0.55f, 0.12f, 0.05f),
            fogDensity = 0.045f,
            groundColor = floatArrayOf(0.18f, 0.1f, 0.08f),
            dialogs = listOf(
                "Guardian: None shall pass! I guard the portal of the volcanic gate!",
                "Jack: I must use the portal to return home. Stand aside!",
                "Guardian: Prove your worth in the fires of battle, Samurai!"
            ),
            enemyCount = 1,
            bossType = "LavaGuardian"
        ),
        StageConfig(
            stageNumber = 13,
            title = "Path of the Ronin",
            subtitle = "Final Duel",
            objective = "Face an elite enemy and continue the journey toward the final confrontation.",
            bgColor = floatArrayOf(0.18f, 0.08f, 0.12f),
            fogColor = floatArrayOf(0.48f, 0.18f, 0.22f),
            fogDensity = 0.03f,
            groundColor = floatArrayOf(0.22f, 0.18f, 0.18f),
            dialogs = listOf(
                "Shadow Ronin: You have traveled far, Jack. But your journey ends in these ruins.",
                "Jack: I have fought through time and space. I will not fail now.",
                "Shadow Ronin: Draw your sword. Let the final duel begin!"
            ),
            enemyCount = 1,
            bossType = "Ronin"
        )
    )
}
