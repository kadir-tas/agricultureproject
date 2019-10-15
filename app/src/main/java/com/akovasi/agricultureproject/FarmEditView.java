package com.akovasi.agricultureproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import com.akovasi.agricultureproject.ShortCut.ShortCut;
import com.akovasi.agricultureproject.datatypes.farmdata.Farm;
import com.akovasi.agricultureproject.datatypes.farmdata.Module;
import com.akovasi.agricultureproject.datatypes.farmdata.Product;
import com.akovasi.agricultureproject.datatypes.farmdata.ProductData;
import com.akovasi.agricultureproject.datatypes.math.Vector2;
import com.akovasi.agricultureproject.vendors.snatik.polygon.Line;
import com.akovasi.agricultureproject.vendors.snatik.polygon.Point;
import com.akovasi.agricultureproject.vendors.snatik.polygon.Polygon;

import java.util.ArrayList;
import java.util.Random;

public class FarmEditView extends android.support.v7.widget.AppCompatImageView implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "FarmEditView";

    private static final float MAX_SCALE_X = 6;
    private static final float MAX_SCALE_Y = 6;

    private static final float MIN_SCALE_X = 1;
    private static final float MIN_SCALE_Y = 1;

    private static final float MIN_PRODUCT_DRAW_DIST_X = 2;
    private static final float MIN_PRODUCT_DRAW_DIST_Y = 2;

    //INTERFACES
    public OnLongClickListener on_long_click_listener;
    public OnProductClick on_product_click_listener;
    public OnModuleClick on_module_click_listener;
    public OnPhaseChange on_phase_change_listener;
    public OnDisplayPhaseChange on_display_phase_change;
    public OnFixupCellClick on_fixup_cell_click;

    //FLAGS
    private boolean allow_putting_data = false;
    private boolean can_remove_data = false;
    private boolean can_fix_cell = false;

    //BOOKEPING
    private EditPhases current_edit_phase;
    private DisplayPhases current_display_phase;
    private Control current_control;

    //GESTURE DETECTORS
    private GestureDetector gesture_detector_compat;
    private ScaleGestureDetector scale_gesture_detector;

    //DATA
    private Farm edited_farm;

    //FOR EDIT PHASE
    private ArrayList<Product> products;
    private Vector2 raw_mpos = new Vector2();

    //FOR OUTLINE DRAW PHASE
    private ArrayList<Vector2> outline_points;
    private ArrayList<Vector2> temp_points;
    private Vector2 start_offset = new Vector2(32, 32);  // HOW FAR IT WILL START DRAWING FROM x and y : for this instance it will start from + 16 , offset from x and y
    private Vector2 current_snapped_point = new Vector2(0, 0); // THIS IS IN NORMALIZED SPACE FOR INSTANCE IF FARM SIZE IS 16 by 16 it will be whole numbers from 0 to 16
    private boolean is_snapped = false;
    private Vector2 mpos = new Vector2(0, 0); //THIS IS IN NORMALIZED SPACE for instance if farm size is 16, x and y compenet is between 0 and 16

    //UTILS AND BOUNDS
    private Canvas canvas_ref; // might need in the futre
    private Vector2 canvas_bounds = new Vector2(0, 0);
    private Vector2 dxdy = new Vector2(0, 0);

    //CAMERA
    private Vector2 scale = new Vector2(1, 1);
    private Vector2 offset = new Vector2(0, 0);
    private Vector2 avg_mpos = new Vector2(0, 0);

    //FOR COLLISION DETECTION
    private Polygon outline_shape;

    //PAINT
    private Paint cell_paint = new Paint();
    private Paint paint = new Paint();
    private Paint text_paint = new Paint();
    private Paint product_paint = new Paint();
    private Paint fixup_cell_paint = new Paint();

    //PRECALCULATED DATA
    /* This represenst the each single cell and wheter it's condition is good or bad and provides data for drawing*/
    private ArrayList<FixupCellData> fixup_cell_data;
    private ArrayList<Line> grid_lines;


    //TIMERS
    private float last_draw_time = -9999;

    public FarmEditView(Context context) {
        super(context);
        init(context);
    }

    public FarmEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FarmEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void load_farm(Farm f, boolean is_display_only) {
        load_farm(f);
        if (is_display_only) change_edit_phase(EditPhases.DISPLAY_ONLY_PHASE);
    }

    public void load_farm(Farm f) {
        if (f == null) {
            Log.v(TAG, "Loaded farm is nulll");
            return;
        }

        Log.v(TAG, f.farm_id + " is loaded");

        if (f.outline_points != null && f.outline_points.size() >= 3 && check_if_outline_is_closed(f.outline_points)) {
            edited_farm = f;
            products = f.products;
            outline_points = f.outline_points;
            change_edit_phase(EditPhases.PRODUCT_PLACEMENT_PHASE);
            current_display_phase = DisplayPhases.DISPLAY_PRODUCTS;

            build_outline_shape();
            initilize_fixup_cell_data();
            initilze_line_grid();
        } else {
            ShortCut.displayMessageToast(getContext(), "Farm data is not valid something went wrong");
        }
    }

    private void build_outline_shape() {
        if (outline_points.size() < 3) return;
        Polygon.Builder builder = Polygon.Builder();

        for (Vector2 v : outline_points) {
            builder.addVertex(v.toPoint());
        }

        outline_shape = builder.build();
    }

    public void save_farm() {
        if (outline_points != null && check_if_outline_is_closed(outline_points)) {
            edited_farm.outline_points = outline_points;
            if (products != null)
                edited_farm.products = products;
            Log.v(TAG, edited_farm.toString());
        } else {
            Toast.makeText(getContext(), "Önce Çizimi Tamamlayın", Toast.LENGTH_LONG).show();
        }

    }

    private void init(Context context) {
        current_control = Control.EDIT;
        outline_points = new ArrayList<>();
        temp_points = new ArrayList<>();
        products = new ArrayList<>();
        fixup_cell_data = new ArrayList<>();
        grid_lines = new ArrayList<>();

        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(8);

        cell_paint.setColor(Color.BLUE);
        cell_paint.setAlpha(120);
        cell_paint.setStyle(Paint.Style.FILL);

        product_paint.setColor(Color.GREEN);
        product_paint.setAlpha(190);
        product_paint.setStyle(Paint.Style.FILL);

        fixup_cell_paint.setColor(Color.GREEN);
        fixup_cell_paint.setAlpha(190);
        fixup_cell_paint.setStyle(Paint.Style.FILL);

        text_paint.setColor(Color.BLACK);
        text_paint.setTextSize(10);
        text_paint.setTextAlign(Paint.Align.CENTER);
        text_paint.setFakeBoldText(true);


        scale_gesture_detector = new ScaleGestureDetector(getContext(), this);
        gesture_detector_compat = new GestureDetector(getContext(), this);
    }


    private void init_bounds(Canvas canvas) {
        this.canvas_ref = canvas;
        canvas_bounds.x = this.canvas_ref.getWidth();
        canvas_bounds.y = this.canvas_ref.getHeight();

        dxdy.x = (canvas_bounds.x + start_offset.x) / edited_farm.size;
        dxdy.y = (canvas_bounds.y + start_offset.y) / edited_farm.size;
    }

    private boolean check_if_outline_is_closed(ArrayList<Vector2> lines) {
        if (lines == null || lines.size() < 3) return false;
        return lines.get(0).equals(lines.get(lines.size() - 1));
    }

    private void finger_up_behaviour_outline_draw_phase(MotionEvent event) {
        //RANGES FROM 0 to matrix lenght
        if (current_control != Control.EDIT || outline_points.size() == 0) return;

        if (outline_points.size() == 1 && temp_points.size() <= 1) {
            outline_points.clear();
        } else if (!outline_points.get(outline_points.size() - 1).equals(mpos.x, mpos.y)) { // IF DIDNT DRAG ALL THE WAYY BUT RETURNED THE BEGING
            outline_points.addAll(temp_points);
            temp_points.clear();

            if (check_if_outline_is_closed(outline_points)) {
                Toast.makeText(getContext(), "State Changed", Toast.LENGTH_LONG).show();
                change_edit_phase(EditPhases.PRODUCT_PLACEMENT_PHASE);
                build_outline_shape();
            }
        }

        is_snapped = false;
    }

    public void undo() {
        /* NOT USED HERE */
    }

    public void clean() {
        products.clear();


        change_edit_phase(EditPhases.PRODUCT_PLACEMENT_PHASE);
        scale.x = 1;
        scale.y = 1;
        offset.x = 0;
        offset.y = 0;
    }

    private void finger_down_behaviour_outline_draw_phase(MotionEvent event) {
        if (current_control != Control.EDIT) return;

        current_snapped_point.x = mpos.x;
        current_snapped_point.y = mpos.y;

        if (outline_points.size() == 0) { // IF THIS IS THE FIRST TIME ADD THIS AS A BEGINING PONT
            //Log.v(TAG, "Add the first point " + current_snapped_point.toString());
            outline_points.add(new Vector2(current_snapped_point));
        }

        if (outline_points.get(outline_points.size() - 1).equals(current_snapped_point)) { // IF I PRESSED THE LAST EDITED PLACE THEN I ALLOW DRAWING
            //Log.v(TAG, "Allow Drawing is setted to true");
            is_snapped = true;
        }
    }

    private void finger_move_behaviour_outline_draw_phase(MotionEvent event) {
        if (current_control != Control.EDIT) {
            temp_points.clear();
            return;
        }

        if (is_snapped) {
            //Log.v(TAG, "Pointer Moved prepared for stuff");
            temp_points.clear(); // MIGHT NOT BE THE BEST WAY HERE
            //THESE VECTORS AND INT'S ARE IN MATRIX SPACE THEY RANGES FROM 0 TO MATRIX LENGHT !!!!!!!
            Vector2 last_point = outline_points.get(outline_points.size() - 1);
            temp_points.add(last_point);
            int diff_x = (int) last_point.x - (int) mpos.x; // how many steps in x
            int diff_y = (int) last_point.y - (int) mpos.y; // how many steps in y
            //Log.v(TAG, "DIFF <" + diff_x + " , " + diff_y + "> Poinst");

            int x = (int) last_point.x;
            int y = (int) last_point.y;
            while (diff_x != 0 || diff_y != 0) {
                //Log.v(TAG, "ADDED TO TEMP <" + x + " , " + y + "> Poinst");

                //TODO: MAKE THIS MORE BEATIFULL
                if (diff_x < 0) {
                    x++;
                    diff_x++;
                }
                if (diff_x > 0) {
                    x--;
                    diff_x--;
                }
                if (diff_y < 0) {
                    y++;
                    diff_y++;
                }
                if (diff_y > 0) {
                    y--;
                    diff_y--;
                }

                if (outline_points.get(0).equals(x, y)) {
                    temp_points.add(new Vector2(x, y));
                    break;
                }
                if (does_outline_points_contains(x, y)) {
                    break;
                }
                temp_points.add(new Vector2(x, y));
            }
        }
    }

    private boolean does_outline_points_contains(int x, int y) {
        for (Vector2 v : outline_points) {
            if (v.equals(x, y)) {
                Log.v(TAG, "CONTAINS");
                return true;
            }
        }
        return false;
    }


    private void draw_line_from_points(Canvas canvas, ArrayList<Vector2> points) {

        Vector2 first_point = null;
        for (Vector2 v : points) {
            if (first_point == null) {
                first_point = v;
            } else {
                float x1 = first_point.x * dxdy.x + start_offset.x;
                float y1 = first_point.y * dxdy.y + start_offset.y;
                float x2 = v.x * dxdy.x + start_offset.x;
                float y2 = v.y * dxdy.y + start_offset.y;
                //Log.v(TAG, "DRAING AT <" + x1 + " , " + y1 + " , " + x2 + " , " + y2 + "> ");
                canvas.drawLine(x1, y1, x2, y2, paint);
                first_point = v;
            }
        }
    }

    private void draw_fixup_cell(Canvas canvas) {
        if (current_display_phase != DisplayPhases.DISPLAY_FIXES) return;
        Path path = new Path();

        for (FixupCellData f : fixup_cell_data) {
            if (f.condition) {
                //IN GOOD CONDITION
                fixup_cell_paint.setColor(Color.GREEN);
            } else {
                //IS NOT IN GOOD CONTDION
                fixup_cell_paint.setColor(Color.RED);
            }
            path.reset();
            path.moveTo(f.points.get(0).x * dxdy.x + start_offset.x, f.points.get(0).y * dxdy.y + start_offset.y);
            for (Vector2 p : f.points) {
                path.lineTo(p.x * dxdy.x + start_offset.x, p.y * dxdy.y + start_offset.y);
            }

            canvas.drawPath(path, fixup_cell_paint);
        }

    }

    private void draw_grids(Canvas canvas) {
        for (Line l : grid_lines) {
            Point start = l.getStart();
            Point end = l.getEnd();
            canvas.drawLine(
                    (float) start.x * dxdy.x + start_offset.x,
                    (float) start.y * dxdy.y + start_offset.y,
                    (float) end.x * dxdy.x + start_offset.x,
                    (float) end.y * dxdy.y + start_offset.y,
                    paint);
        }
    }

    private void draw_product_cells(Canvas canvas, boolean show_module_ids) {
        if (current_display_phase != DisplayPhases.DISPLAY_PRODUCTS) return;

        Path path = new Path();

        for (Product product : products) {
            path.reset();
            path.moveTo(product.points.get(0).x * dxdy.x + start_offset.x, product.points.get(0).y * dxdy.y + start_offset.y);
            Vector2 avg = new Vector2();
            for (Vector2 point : product.points) {
                path.lineTo(point.x * dxdy.x + start_offset.x, point.y * dxdy.y + start_offset.y);
                avg = avg.add(new Vector2(point.x * dxdy.x + start_offset.x, point.y * dxdy.y + start_offset.y));
            }
            avg = avg.div(product.points.size());


            byte[] bytes = product.product_data.product_id.getBytes();
            int l = bytes.length;

            product_paint.setARGB(255, bytes[3 % l] * bytes[0 % l], bytes[2 % l] * bytes[1 % l], bytes[3 % l] + bytes[4 % l]);

            canvas.drawPath(path, product_paint);

            if (show_module_ids) {
                canvas.drawText(product.product_data.product_name, avg.x, avg.y, text_paint);
            }

        }
    }

    private void draw_module_cells(Canvas canvas, boolean show_module_ids) {
        if (current_display_phase != DisplayPhases.DISPLAY_MODULES) return;

        Path path = new Path();

        for (Module module : edited_farm.modules) {
            path.reset();
            path.moveTo(module.points.get(0).x * dxdy.x + start_offset.x, module.points.get(0).y * dxdy.y + start_offset.y);
            Vector2 avg = new Vector2();
            for (Vector2 point : module.points) {
                path.lineTo(point.x * dxdy.x + start_offset.x, point.y * dxdy.y + start_offset.y);
                avg = avg.add(new Vector2(point.x * dxdy.x + start_offset.x, point.y * dxdy.y + start_offset.y));
            }
            avg = avg.div(module.points.size());

            canvas.drawPath(path, cell_paint);

            if (show_module_ids) {
                canvas.drawText(module.module_id, avg.x, avg.y, text_paint);
            }

        }
    }

    private ArrayList<Vector2> cast_fill(float x, float y) {
        float mx = x;
        float my = y;

        ArrayList<Vector2> m = new ArrayList<>();

        Vector2 center = new Vector2((float) Math.floor(mx) + 0.5f, (float) Math.floor(my) + 0.5f);

        /*
         *       *********
         *       **  a  **
         *       **    * *
         *       * *  *  *
         *       *d **  b*
         *       *  **   *
         *       * *  *  *
         *       **  c  **
         *       *********
         * */

        boolean a = false;
        boolean b = false;
        boolean c = false;
        boolean d = false;

        if (outline_shape.contains(center.add(Vector2.up.mult(0.2f)).toPoint())) {
            a = true;
        }
        if (outline_shape.contains(center.add(Vector2.right.mult(0.2f)).toPoint())) {
            b = true;
        }
        if (outline_shape.contains(center.add(Vector2.down.mult(0.2f)).toPoint())) {
            c = true;
        }
        if (outline_shape.contains(center.add(Vector2.left.mult(0.2f)).toPoint())) {
            d = true;
        }


        if (a & b & c & d) {
            m.add(new Vector2(center.add(new Vector2(-0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, 0.5f))));
            m.add(new Vector2(center.add(new Vector2(-0.5f, 0.5f))));
            Log.v(TAG, "ABCD");
        } else if (a & b) {
            m.add(new Vector2(center.add(new Vector2(-0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, 0.5f))));
            Log.v(TAG, "AB");
        } else if (a & d) {
            m.add(new Vector2(center.add(new Vector2(-0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(-0.5f, 0.5f))));
            Log.v(TAG, "AD");
        } else if (c & b) {
            m.add(new Vector2(center.add(new Vector2(0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, 0.5f))));
            m.add(new Vector2(center.add(new Vector2(-0.5f, 0.5f))));
            Log.v(TAG, "CB");
        } else if (c & d) {
            m.add(new Vector2(center.add(new Vector2(-0.5f, -0.5f))));
            m.add(new Vector2(center.add(new Vector2(0.5f, 0.5f))));
            m.add(new Vector2(center.add(new Vector2(-0.5f, 0.5f))));
            Log.v(TAG, "CD");
        }


        return m;
    }

    public Product remove_data_last_selected_place() {
        Product removed_product = null;
        Product m = new Product();
        m.points = cast_fill(raw_mpos.x, raw_mpos.y);
        if (m.points.size() >= 2 && can_remove_data) {
            int index = -1;
            if ((index = products.indexOf(m)) >= 0) {
                removed_product = products.get(index);
                products.remove(index);
                can_remove_data = false;
            }
        }

        return removed_product;
    }

    public void fixup_cell_in_last_selected_place() {
        FixupCellData f = new FixupCellData();
        f.points = cast_fill(raw_mpos.x, raw_mpos.y);
        int index = -1;
        if ((index = fixup_cell_data.indexOf(f)) >= 0) {
            fixup_cell_data.get(index).condition = true;
            //initilize_fixup_cell_data();
        }
    }

    public void put_data_onto_last_selected_place(ProductData productData) {
        if (!allow_putting_data) return;

        Product m = new Product();
        m.points = cast_fill(raw_mpos.x, raw_mpos.y);

        if (m.points.size() <= 2) {
            Log.v(TAG, "Module cannot be added");
            return;
        } else {
            Log.v(TAG, productData.product_id + " is added to " + m.toString());
            m.product_data = productData;
            products.add(m);
        }
    }

    private void draw_dot_matrix(Canvas canvas, float radius, Paint paint) {
        for (int y = 0; y < edited_farm.size; y++) {
            for (int x = 0; x < edited_farm.size; x++) {
                float x0 = x * dxdy.x + start_offset.x;
                float y0 = y * dxdy.y + start_offset.y;
                if (outline_points.contains(new Vector2(x, y)))
                    canvas.drawCircle(x0, y0, radius * 1.8f, paint);
                else if (current_edit_phase == EditPhases.OUTLINE_DRAW_PHASE)
                    canvas.drawCircle(x0, y0, radius, paint);

            }
        }
    }

    //TODO: REFACTOR HERE PLZ
    private void finger_down_behaviour_product_placement_phase(MotionEvent event) {

        Product m = new Product();
        m.points = cast_fill(raw_mpos.x, raw_mpos.y);
        FixupCellData f = new FixupCellData();
        f.points = m.points;

        if (m.points.size() <= 2) {
            allow_putting_data = false;
        } else {
            if (current_display_phase == DisplayPhases.DISPLAY_FIXES) {
                if (!fixup_cell_data.get(fixup_cell_data.indexOf(f)).condition) {
                    can_fix_cell = true;
                } else {
                    can_fix_cell = false;
                }
            }
            if (current_display_phase == DisplayPhases.DISPLAY_PRODUCTS) {
                int index = products.indexOf(m);
                if (index >= 0) {
                    allow_putting_data = false;
                    can_remove_data = true;
                } else {
                    allow_putting_data = true;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        avg_mpos = avg_mpos.mult(0);
        for (int i = 0; i < event.getPointerCount(); i++) {
            avg_mpos = avg_mpos.add(new Vector2(event.getX(i), event.getY(i)));
        }
        avg_mpos = avg_mpos.div(event.getPointerCount());

        if (current_edit_phase == EditPhases.DISPLAY_ONLY_PHASE) return super.onTouchEvent(event);
        //RESET THE CONTOL PHASES
        this.scale_gesture_detector.onTouchEvent(event);
        this.gesture_detector_compat.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();
        raw_mpos.x = (x - start_offset.x * scale.x - offset.x) / (dxdy.x * scale.x);
        raw_mpos.y = (y - start_offset.y * scale.y - offset.y) / (dxdy.y * scale.y);

        //Log.v(TAG , "Dist to center " + Vector2.distance(new Vector2(x , y) , new Vector2(canvas_bounds.x / 2 , canvas_bounds.y / 2)));

        mpos.x = (int) Math.round((x - start_offset.x * scale.x - (offset.x)) / (dxdy.x * scale.x));
        mpos.y = (int) Math.round((y - start_offset.y * scale.y - (offset.y)) / (dxdy.y * scale.y));

        if (mpos.x >= 0 && mpos.x < edited_farm.size && mpos.y >= 0 && mpos.y < edited_farm.size) {

            if (current_edit_phase == EditPhases.OUTLINE_DRAW_PHASE) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        current_control = Control.EDIT;
                        finger_up_behaviour_outline_draw_phase(event);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        finger_down_behaviour_outline_draw_phase(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        finger_move_behaviour_outline_draw_phase(event);
                        break;
                }
            } else if (current_edit_phase == EditPhases.PRODUCT_PLACEMENT_PHASE) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        current_control = Control.EDIT;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        finger_down_behaviour_product_placement_phase(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                }
            }
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas_ref == null) {
            init_bounds(canvas); // RUNS ONLY ONE IN THE BEGGING
        }

        canvas.save();
//        canvas.translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
        canvas.translate(offset.x, offset.y);
        canvas.scale(scale.x, scale.y);
//        canvas.translate(-canvas.getWidth() / 2, -canvas.getHeight() / 2);

        if (current_edit_phase == EditPhases.OUTLINE_DRAW_PHASE) {
            draw_dot_matrix(canvas, 8, paint);
            if (is_snapped) {
                draw_line_from_points(canvas, temp_points);
            }

        } else if (current_edit_phase == EditPhases.PRODUCT_PLACEMENT_PHASE || current_edit_phase == EditPhases.DISPLAY_ONLY_PHASE) {
            draw_grids(canvas);
            draw_module_cells(canvas, true);
            draw_product_cells(canvas, true);
            draw_fixup_cell(canvas);
        }

//        canvas.drawCircle(raw_mpos.x * dxdy.x + start_offset.x, raw_mpos.y * dxdy.y + start_offset.y, 16, paint);
        draw_line_from_points(canvas, outline_points);

        canvas.restore();
        invalidate();
        super.onDraw(canvas);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // Log.v(TAG , "onDown");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        //  Log.v(TAG , "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Log.v(TAG , "onSingleTapUp");
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //Log.v(TAG , "onScroll " + e1.getPointerCount() + " , " + e2.getPointerCount());

        if ((e1.getPointerCount() == 1 && e2.getPointerCount() == 2 || e1.getPointerCount() == 2 && e2.getPointerCount() == 1)) {
            current_control = Control.DRAG;
            offset.x -= distanceX;
            offset.y -= distanceY;
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        on_long_click_listener.onLongClick(this);
        //Log.v(TAG , "onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //Log.v(TAG , "Fling " + e1.toString() + " , " + e2.toString() + " <" + velocityX + "," + velocityY + ">");
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // Log.v(TAG , "onSingleTapConfiremd");
        if (current_display_phase == DisplayPhases.DISPLAY_MODULES) {
            Module p = new Module();
            p.points = cast_fill(raw_mpos.x, raw_mpos.y);
            int index = -1;
            if ((index = edited_farm.modules.indexOf(p)) >= 0) {
                on_module_click_listener.on_module_click(edited_farm.modules.get(index).module_id);
            }
        } else if (current_display_phase == DisplayPhases.DISPLAY_MODULES) {

        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        //TODO: ALLOW SCROLLING
        // Log.v(TAG , "onDoubleTap");
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        //Log.v(TAG, "onScale " + scf);
        float scf = detector.getScaleFactor();
        boolean cannot_zoom = false;
        current_control = Control.ZOOM;

        scale.x *= scf;
        scale.y *= scf;

//        Log.v(TAG, scale.toString());

        if (scale.x > MAX_SCALE_X) {
            scale.x = MAX_SCALE_X;
            cannot_zoom = true;
        }
        if (scale.x < MIN_SCALE_X) {
            scale.x = MIN_SCALE_X;
            cannot_zoom = true;
        }

        if (scale.y > MAX_SCALE_Y) {
            scale.y = MAX_SCALE_Y;
            cannot_zoom = true;
        }
        if (scale.y < MIN_SCALE_Y) {
            scale.y = MIN_SCALE_Y;
            cannot_zoom = true;
        }

        if (!cannot_zoom) {
            float d = detector.getCurrentSpan() - detector.getPreviousSpan();
            Vector2 temp = avg_mpos.sub(offset).normalized();
            offset.x -= temp.x * d * scale.x;
            offset.y -= temp.y * d * scale.y;
        }

        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        //Log.v(TAG , "onScaleBegin");
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        // Log.v(TAG , "onScaleEnd");
    }

    public Farm getEdited_farm() {
        return edited_farm;
    }

    public boolean isCan_fix_cell() {
        return can_fix_cell;
    }

    public boolean isAllow_putting_data() {
        return allow_putting_data;
    }

    private void change_edit_phase(EditPhases nextPhase) {
        EditPhases prev = current_edit_phase;
        current_edit_phase = nextPhase;
        if (on_phase_change_listener == null) return;
        on_phase_change_listener.on_phase_change(prev, current_edit_phase);
    }

    private void change_display_phase(DisplayPhases nextPhase) {
        DisplayPhases prev = current_display_phase;
        current_display_phase = nextPhase;
        if (on_display_phase_change == null) return;
        on_display_phase_change.on_display_phase_change(prev, current_display_phase);
    }

    public EditPhases getCurrent_edit_phase() {
        return current_edit_phase;
    }

    public DisplayPhases getCurrent_display_phase() {
        return current_display_phase;
    }

    public boolean isCan_remove_data() {
        return can_remove_data;
    }

    public void next_display_phase() {
        change_display_phase(DisplayPhases.values()[(current_display_phase.ordinal() + 1) % DisplayPhases.values().length]);
    }

    public void initilize_fixup_cell_data() {
        Random random = new Random();
        for (int y = 0; y < edited_farm.size; y++) {
            for (int x = 0; x < edited_farm.size; x++) {
                Product p = new Product();
                p.points = cast_fill(x + 0.5f, y + 0.5f);
                if (p != null && p.points.size() >= 3) {
                    FixupCellData f = new FixupCellData();
                    f.points = p.points;
                    f.condition = random.nextBoolean();
                    fixup_cell_data.add(f);
                }
            }
        }
    }

    public void initilze_line_grid() {
        for (int y = 0; y < edited_farm.size; y++) {
            for (int x = 0; x < edited_farm.size; x++) {
                float x0 = x;
                float y0 = y;
                if (outline_shape.contains(new Point(x + 0.5f, y))) {
                    grid_lines.add(new Line(new Point(x0, y0), new Point((x + 1), y0)));
                }
                if (outline_shape.contains(new Point(x, y + 0.5f))) {
                    grid_lines.add(new Line(new Point(x0, y0), new Point(x0, (y + 1))));
                }
            }
        }
    }

    public enum EditPhases {OUTLINE_DRAW_PHASE, PRODUCT_PLACEMENT_PHASE, FIX_PHASE, DISPLAY_ONLY_PHASE}

    public enum DisplayPhases {DISPLAY_MODULES, DISPLAY_PRODUCTS, DISPLAY_FIXES}

    private enum Control {EDIT, ZOOM, DRAG}

    public interface OnProductClick {
        void on_product_click(ProductData productData);
    }

    public interface OnModuleClick {
        void on_module_click(String module_id);
    }

    public interface OnFixupCellClick {
        void on_fixup_cell_click(FixupCellData fixupCellData);
    }

    public interface OnPhaseChange {
        void on_phase_change(EditPhases prev_phase, EditPhases current_phase);
    }

    public interface OnDisplayPhaseChange {
        void on_display_phase_change(DisplayPhases prev_phase, DisplayPhases current_phase);
    }

    //Represents a cell that can be fixedup or not
    private class FixupCellData {
        public ArrayList<Vector2> points;
        /* false: BAD , true: Good */
        public boolean condition = false;

        public FixupCellData() {
            points = new ArrayList<>();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FixupCellData) {
                FixupCellData m = (FixupCellData) obj;
                boolean flag = false;
                for (Vector2 v1 : m.points) {
                    flag = false;
                    for (Vector2 v2 : points) {
                        if (v1.equals(v2)) {
                            flag = true;
                        }
                    }
                    if (flag == false) {
                        return false;
                    }
                }

                return true;
            }
            return false;
        }
    }
}
