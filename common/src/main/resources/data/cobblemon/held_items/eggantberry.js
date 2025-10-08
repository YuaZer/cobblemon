{
	name: "Eggant Berry",
	spritenum: 0,
	isBerry: true,
	naturalGift: {
		basePower: 80,
		type: "Normal"
	},
	onUpdate(pokemon) {
		if (pokemon.volatiles['attract']) {
			pokemon.eatItem();
		}
	},
	onEat(pokemon) {
		pokemon.removeVolatile('attract');
		this.add('-end', pokemon, 'move: Attract', '[from] item: Eggant Berry');
	},
	num: -101,
	gen: 3,
	isNonstandard: "Past"
}