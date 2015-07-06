package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.SparseArray;
import com.joanzapata.tilesview.TileRenderer;

import static com.joanzapata.tilesview.TilesView.TILE_SIZE;

public class TilePool {

    private TileRenderer tileRenderer;

    private SparseArray<Tile[][]> tilesByZoomLevel;

    private int tilesBackgroundColor;

    public TilePool(int tilesBackgroundColor) {
        this.tilesBackgroundColor = tilesBackgroundColor;
        this.tilesByZoomLevel = new SparseArray<Tile[][]>();
    }

    public Bitmap getTile(int zoomLevel, int xIndex, int yIndex, float contentWidth, float contentHeight) {

        // Don't try to render anything if there's not tile renderer
        if (tileRenderer == null) return null;

        // Get the tiles array for the given zoom (or create it)
        Tile[][] tiles = tilesByZoomLevel.get(zoomLevel);
        if (tiles == null) {
            // Math.ceil(contentWidth * zoom / TILE_SIZE) - 1
            int xCells = (int) Math.ceil(contentWidth * (zoomLevel / 10f) / TILE_SIZE);
            int yCells = (int) Math.ceil(contentHeight * (zoomLevel / 10f) / TILE_SIZE);
            tiles = new Tile[xCells][yCells];
            tilesByZoomLevel.put(zoomLevel, tiles);
        }

        // Make sure the requested tile is not out of bounds
        if (xIndex < 0 || yIndex < 0 || xIndex >= tiles.length || yIndex >= tiles[0].length)
            return null;

        // Get it
        Tile tile = tiles[xIndex][yIndex];

        // If null, create it
        if (tile == null) {
            tile = new Tile(zoomLevel, xIndex, yIndex);
            tiles[xIndex][yIndex] = tile;

            // Create its bitmap
            Bitmap bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(tilesBackgroundColor);
            float zoom = zoomLevel / 10f;
            tileRenderer.renderTile(canvas,
                    xIndex * TILE_SIZE / zoom / contentWidth,
                    yIndex * TILE_SIZE / zoom / contentHeight,
                    TILE_SIZE / zoom / contentWidth,
                    TILE_SIZE / zoom / contentHeight,
                    contentWidth, contentHeight);
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
