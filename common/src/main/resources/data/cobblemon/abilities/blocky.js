// Test file, remove before merge
{
    onStart(pokemon) {
      let activated = false;
      for (const target of pokemon.adjacentFoes()) {
        if (!activated) {
          this.add("-ability", pokemon, "Intimidate", "boost");
          activated = true;
        }
        if (target.volatiles["substitute"]) {
          this.add("-immune", target);
        } else {
          this.boost({ atk: -1 }, target, pokemon, null, true);
        }
      }
    },
    flags: {},
    name: "Blocky",
    rating: 3.5,
    num: 22
}