# Not usable at the moment.

**TilesView** is an Android widget that is able to display a **very huge image** and make it **browsable**. It works by cutting them in small tiles. The only thing it needs from you is to define how to fill those tiles. It then takes care of threading, recycling tiles, scrolling and zooming for you. 

### Story

We had a huge SVG of a big airport indoor map, with lots of details, and we needed to make it browsable to the user. At first we used [AndroidSVG](http://bigbadaboom.github.io/androidsvg/), a library that reads the SVG file and exposes a method `renderToCanvas(Canvas);`. But the result was really bad: drawing the whole SVG on every frame dropped the framerate down to barely **5 FPS**. It has nothing to do with the lib, there's no way it could render faster since the SVG has thousands of elements including complexe shapes, sometimes transparent, or using gradients, etc...

We thought it could render a lot faster if we render the SVG to a bitmap once and just draw the bitmap on every frame. But this has **2 problems**: the bitmap would **change size** when the users zooms in and out, and it would get **too big** on higher zoom levels.

So I started working on **tilesview**, with the help of [@NicolasPoirier](https://github.com/NicolasPoirier). It basically cuts whatever you need to display in **tiles**. Tiles are 256x256 images, created on a background thread when needed, and reused as soon as they get offscreen. When the user zooms in, new tiles are rendered with the appropriate scale, but they are still 256x256. **All it needs from you is then to know how to fill those tiles.**

![Scale 1](/graphics/scale1.jpg)

![Scale 2](/graphics/scale2.jpg)

In the example above, you can see the strength of this method: it only needs to build and draw **at most 6 tiles on screen at any given time**. Of course that depends on the screen size, it would probably be 30 or 40 on a real device. Instead of drawing a very huge image or a very complex SVG, the GPU only needs to draw those 30-40 small images, and the GPU is **very good** at it. Using this technique, we reached **60 FPS** again!

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
// This is not actually related to the lib, but in this example we want
// to read a huge JPG file, so we'll need a BitmapRegionDecoder to read
// some parts of it when requested in the adapter.
BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(
  context.getResources().getAssets().open("world.jpg"), false);
                    
// Create and set the adapter.
TilesView tilesView = (TilesView) findViewById(R.id.tilesView);
tilesView.setAdapter(new BigMapAdapter(decoder));
```

And implement the adapter, subclassing `FixedSizeAdapter`.

```java
public class BigMapAdapter extends FixedSizeAdapter {
    private final BitmapRegionDecoder decoder;

    public Adapter2FixedSize(BitmapRegionDecoder decoder) {
        super(decoder.getWidth(), decoder.getHeight());
        this.decoder = decoder;
    }

    @Override
    protected void drawTile(Canvas canvas, RectF sourceRectF, RectF destRectF) {
        // Decode the part of the image we need to render in this tile    
        // and draw this part on the tile, at the bounds specified by destRectF
        Bitmap tmpBitmap = decoder.decodeRegion(sourceRectF);
        canvas.drawBitmap(tmpBitmap, null, destRectF, null);
    }
}
```

### Contributors

* Joan Zapata [@JoanZapata](https://github.com/JoanZapata)
* Nicolas Poirier [@NicolasPoirier](https://github.com/NicolasPoirier)

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
