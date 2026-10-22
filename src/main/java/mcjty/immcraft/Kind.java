package mcjty.immcraft;

public enum Kind {
    CHEST("chest",12), CUPBOARD("cupboard",16), SHELF("shelf",16), BOOKSHELF("bookshelf",27),
    FURNACE("furnace",3), WORKBENCH("workbench",4), WORKBENCH_SEC("workbench_sec",1),
    DISPLAY("in_world_placer",4), WALL_DISPLAY("in_world_vertical_placer",4), STICKS("sticks",1), ROCK("rock",0),BOOKSTAND("bookstand",1);
    public final String id;
    public final int slots;
    Kind(String id, int slots) { this.id=id; this.slots=slots; }
    public boolean display() { return this == DISPLAY || this == WALL_DISPLAY; }
    public boolean wall() { return this == WALL_DISPLAY || this == CUPBOARD || this == SHELF || this == BOOKSHELF; }
    public boolean lid() { return this == CHEST || this == CUPBOARD; }
}
