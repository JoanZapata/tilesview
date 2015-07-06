package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.SparseArray;
import com.joanzapata.tilesview.TileRenderer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.joanzapata.tilesview.TilesView.TILE_SIZE;

public class TilePool {

    /** Thread pool executor which will render everything */
    private ExecutorService executorService;

    /** Callback for rendered tiles */
    private TilePoolListener tilePoolListener;

    /** User class used to draw content on tiles */
    private TileRenderer tileRenderer;

    private SparseArray<Tile[][]> tilesByZoomLevel;

    private int tilesBackgroundColor;

    public TilePool(int tilesBackgroundColor, TilePoolListener tilePoolListener) {
        this.tilePoolListener = tilePoolListener;
        this.tilesBackgroundColor = tilesBackgroundColor;
        this.tilesByZoomLevel = new SparseArray<Tile[][]>();
    }

    public Bitmap getTile(final int zoomLevel, final int xIndex, final int yIndex, final float contentWidth, final float contentHeight) {

        // Don't try to render anything if there's no tile renderer
        if (tileRenderer == null) return null;

        // Get the tiles array for the given zoom (or create it)
        Tile[][] tiles = tilesByZoomLevel.get(zoomLevel);
        if (tiles == null) {
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

        // If currently rendering, ignore
        if (tile != null && tile.getState() == Tile.STATE_RENDERING) {
            return null;
        }

        // If null request a rendering
        if (tile == null) {

            tile = new Tile();
            tile.setState(Tile.STATE_RENDERING);
            tiles[xIndex][yIndex] = tile;

            final Tile finalTile = tile;
            executorService.submit(new Runnable() {
                @Override
                public void run() {

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

                    finalTile.setBitmap(bitmap);
                    finalTile.setState(Tile.STATE_RENDERED);
                    tilePoolListener.onTileRendered(finalTile);

                }
            });

        }

        // Return the bitmap of the created tile
        return tile.getBitmap();
    }

    public void setTileRenderer(TileRenderer tileRenderer, boolean threadSafe) {
        if (executorService != null)
            executorService.shutdownNow();
        int nbCores = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(threadSafe ? nbCores : 1);
        this.tileRenderer = tileRenderer;
    }

    public interface TilePoolListener {
        void onTileRendered(Tile tile);
    }
}
