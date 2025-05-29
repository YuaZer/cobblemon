// Test file, remove before merge
{
    num: 920,
    accuracy: true,
    basePower: 40,
    category: "Special",
    name: "Craft",
    pp: 10,
    priority: 1,
    flags: { nonsky: 1, metronome: 1 },
    terrain: "electricterrain",
    condition: {
      duration: 3,
      durationCallback(source, effect) {
        if (source?.hasItem("terrainextender")) {
          return 8;
        }
        return 3;
      },
      onSetStatus(status, target, source, effect) {
        if (status.id === "slp" && target.isGrounded() && !target.isSemiInvulnerable()) {
          if (effect.id === "yawn" || effect.effectType === "Move" && !effect.secondaries) {
            this.add("-activate", target, "move: Electric Terrain");
          }
          return false;
        }
      },
      onTryAddVolatile(status, target) {
        if (!target.isGrounded() || target.isSemiInvulnerable())
          return;
        if (status.id === "yawn") {
          this.add("-activate", target, "move: Electric Terrain");
          return null;
        }
      },
      onBasePowerPriority: 6,
      onBasePower(basePower, attacker, defender, move) {
        if (move.type === "Electric" && attacker.isGrounded() && !attacker.isSemiInvulnerable()) {
          this.debug("electric terrain boost");
          return this.chainModify([5325, 4096]);
        }
      },
      onFieldStart(field, source, effect) {
        if (effect?.effectType === "Ability") {
          this.add("-fieldstart", "move: Electric Terrain", "[from] ability: " + effect.name, "[of] " + source);
        } else {
          this.add("-fieldstart", "move: Electric Terrain");
        }
      },
      onFieldResidualOrder: 27,
      onFieldResidualSubOrder: 7,
      onFieldEnd() {
        this.add("-fieldend", "move: Electric Terrain");
      }
    },
    secondary: {
          chance: 100,
          boosts: {
            spe: -1
          }
        },
    target: "allAdjacentFoes",
    type: "Electric",
    zMove: { boost: { spe: 1 } },
    contestType: "Clever"
}