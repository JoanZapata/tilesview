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

As you can see there's always the same number of tiles on the screen, but they render the image at different scale. Now let's see how to implement it.

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

That's it!

### License

```
Copyright 2015 Joan Zapata

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
