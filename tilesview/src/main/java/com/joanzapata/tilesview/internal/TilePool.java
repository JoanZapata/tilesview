package com.joanzapata.tilesview.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.SparseArray;
import com.joanzapata.tilesview.TileRenderer;

import static com.joanzapata.tilesview.TilesView.TILE_SIZE;

public class TilePool {

    private static final float PLACEHOLDER_RATIO = 1f;

    /** Thread pool executor which will render everything */
    private LIFOExecutor executor;

    /** Callback for rendered tiles */
    private TilePoolListener tilePoolListener;

    /** User class used to draw content on tiles */
    private TileRenderer tileRenderer;

    private SparseArray<Tile[][]> tilesByZoomLevel;

    private int tilesBackgroundColor;

    private Runnable placeholderRunnable;

    private Bitmap placeholder;

    private int maxTasks;

    public TilePool(int tilesBackgroundColor, TilePoolListener tilePoolListener) {
        this.tilePoolListener = tilePoolListener;
        this.tilesBackgroundColor = tilesBackgroundColor;
        this.maxTasks = 1;
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
            executor.submit(new TileRenderingTask(tile,
                    xIndex, yIndex, zoomLevel,
                    contentWidth, contentHeight));

        }

        // Return the bitmap of the created tile
        return tile.getBitmap();
    }

    public Bitmap getPlaceholder(final float contentWidth, final float contentHeight) {
        if (tileRenderer == null || placeholderRunnable != null || contentWidth == 0 || contentHeight == 0)
            return null;

        if (placeholder != null)
            return placeholder;

        placeholderRunnable = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(
                        (int) (contentWidth * PLACEHOLDER_RATIO),
                        (int) (contentHeight * PLACEHOLDER_RATIO),
                        Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(tilesBackgroundColor);
                tileRenderer.renderTile(canvas,
                        0f, 0f, 1f, 1f,
                        contentWidth, contentHeight);
                placeholder = bitmap;
                placeholderRunnable = null;
            }
        };

        executor.submit(placeholderRunnable);
        return placeholder;
    }

    public void setTileRenderer(TileRenderer tileRenderer, boolean threadSafe) {

        // Stop existing executor service
        if (executor != null)
            executor.shutdownNow();

        // Reset all tiles
        this.tilesByZoomLevel = new SparseArray<Tile[][]>();

        int nbCores = Runtime.getRuntime().availableProcessors();
        executor = new LIFOExecutor(threadSafe ? nbCores : 1);
        executor.setCapacity(maxTasks);
        this.tileRenderer = tileRenderer;
    }

    public void setMaxTasks(int maxTasks) {
        this.maxTasks = maxTasks;
        if (executor != null) executor.setCapacity(maxTasks);
    }

    public interface TilePoolListener {
        void onTileRendered(Tile tile);
    }

    private class TileRenderingTask implements Runnable, LIFOExecutor.Cancellable {

        final Tile tile;
        final int xIndex, yIndex, zoomLevel;
        final float contentWidth, contentHeight;

        public TileRenderingTask(Tile tile, int xIndex, int yIndex, int zoomLevel, float contentWidth, float contentHeight) {
            this.tile = tile;
            this.xIndex = xIndex;
            this.yIndex = yIndex;
            this.zoomLevel = zoomLevel;
            this.contentWidth = contentWidth;
            this.contentHeight = contentHeight;
        }

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

            tile.setBitmap(bitmap);
            tile.setState(Tile.STATE_RENDERED);
            tilePoolListener.onTileRendered(tile);

        }

        @Override
        public void cancel() {

            // Remove the tile
            tilesByZoomLevel.get(zoomLevel)[xIndex][yIndex] = null;

        }
    }
}
