# Not usable at the moment.

### Story

We had a huge SVG of a indoor map for a big airport, and there was a lot of details in it. We needed a way to display the whole map on the screen, and then allow the user to zoom in and drag the map to see all the details. At first we used a library that reads the SVG file and expose a method `svg.renderToCanvas(canvas);`, and we managed the zoom / scroll ourself. The result was really bad: drawing the whole SVG on every frame dropped the framerate down to barely **5 FPS**.

I created this lib with the help of @NicolasPoirier, it basically cuts whatever you need to display in **small tiles**. Tiles are 256x256 images, created on a background thread, and reused whenever they're offscreen to limit the memory footprint. When the user zooms into the image, it renders new tiles, still 256x256, but it asks you to fill them with smaller areas of the content. **All it needs from you is then to know how to fill those tiles.**

It only needs to draw **at most 30-40 tiles** on screen, depending on the screen size, at any given time. It's almost the only thing that needs to be kept in memory. Instead of drawing a very huge image or, like before, a very complex SVG with thousands of element, the GPU only needs to draw those 30-40 small images, and the GPU is **very good** at it. Using this technique, we reached **60 FPS** again!

### Concept

I can't supply the original SVG here for confidentiality reasons, but let's use [this huge world map image (11730x6351)](/tilesview-demo/src/main/assets/world.jpg) as an example.

This is what a tile looks like at scale 1.0. 
![Scale 1](/graphics/scale1.jpg)

And now at scale 2.0. 
![Scale 2](/graphics/scale2.jpg)

### Usage

Put a `TilesView` in your layout.

```xml
<com.joanzapata.tilesview.TilesView
  android:id="@+id/tilesView"
  android:layout_width="wrap_content"
  android:layout_height="wrap_content" />
```

Programatically set an adapter to it.

```java
// Decode the big image (in real world this should be done on a background thread)
BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(context.getResources()
                    .getAssets().open("world.jpg"), false);
                    
// Create and set the adapter
TilesView tilesView = (TilesView) findViewById(R.id.tilesView);
tilesView.setAdapter(new BigMapAdapter(decoder));
```

And implement the adapter, subclassing `FixedSizeAdapter`.

```java
public class BigMapAdapter extends FixedSizeAdapter {

    private final BitmapRegionDecoder decoder;

    public Adapter2FixedSize(BitmapRegionDecoder decoder) {
        // The FixedSizeAdapter need to know the size of the content
        super(decoder.getWidth(), decoder.getHeight());
        this.decoder = decoder;
    }

    @Override
    protected void drawTile(Canvas canvas, RectF sourceRectF, RectF destRectF) {

        // Decode the part of the image we need to render in this tile    
        Bitmap tmpBitmap = decoder.decodeRegion(sourceRectF);
        
        // Draw this part on the tile, at the bounds specified by destRectF
        canvas.drawBitmap(tmpBitmap, null, destRectF, null);
        
        // This decoded part is no longer needed
        tmpBitmap.recycle();
    }
}
```


