package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.SparseArray;
import com.joanzapata.tilesview.TileRenderer;

import static com.joanzapata.tilesview.TilesView.TILE_SIZE;

public class TilePool {

    private TileRenderer tileRenderer;

    private SparseArray<Tile[][]> tilesByZoomLevel;

    public TilePool() {
        tilesByZoomLevel = new SparseArray<Tile[][]>();
    }

    public Bitmap getTile(int zoomLevel, int x, int y, int screenWidth, int screenHeight) {

        // Get the tiles array for the given zoom (or create it)
        Tile[][] tiles = tilesByZoomLevel.get(zoomLevel);
        if (tiles == null) {
            tiles = new Tile
                    [(int) Math.ceil(screenWidth * (zoomLevel / 10f) / TILE_SIZE) + 1]
                    [(int) Math.ceil(screenHeight * (zoomLevel / 10f) / TILE_SIZE) + 1];
            tilesByZoomLevel.put(zoomLevel, tiles);
        }

        // Make sure the requested tile is not out of bounds
        if (x < 0 || y < 0 || x >= tiles.length || y >= tiles[0].length)
            return null;

        // Get it
        Tile tile = tiles[x][y];

        // If null, create it
        if (tile == null) {
            tile = new Tile(zoomLevel, x, y);
            tiles[x][y] = tile;

            // Create its bitmap
            Bitmap bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            float zoom = zoomLevel / 10f;
            tileRenderer.renderTile(canvas,
                    x * TILE_SIZE / zoom / screenWidth,
                    y * TILE_SIZE / zoom / screenHeight,
                    TILE_SIZE / zoom / screenWidth,
                    TILE_SIZE / zoom / screenHeight,
                    screenWidth, screenHeight);
            tile.bitmap = bitmap;
        }

        // Return the bitmap of the created tile
        return tile.bitmap;
    }

    public void setTileRenderer(TileRenderer tileRenderer) {
        this.tileRenderer = tileRenderer;
    }

    public TileRenderer getTileRenderer() {
        return tileRenderer;
    }

}
