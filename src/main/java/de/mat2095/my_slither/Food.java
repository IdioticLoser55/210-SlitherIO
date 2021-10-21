package de.mat2095.my_slither;

//class used for food stats and placement.
class Food {

    final int x, y;
    private final double size;
    private final double rsp;
    private final long spawnTime;

    Food(int x, int y, double size, boolean fastSpawn) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.rsp = fastSpawn ? 4 : 1;
        spawnTime = System.currentTimeMillis();
    }

    //doesn't really seem to be used.
    double getSize() {
        return size;
    }

    //returns the radius of the food. think what is happening is that food grows up from nothing into its full size and this is what scales it. Not sure though.
    double getRadius() {
        double fillRate = rsp * (System.currentTimeMillis() - spawnTime) / 1200;
        if (fillRate >= 1) {
            return size;
        } else {
            return (1 - Math.cos(Math.PI * fillRate)) / 2 * size;
        }
    }
}
