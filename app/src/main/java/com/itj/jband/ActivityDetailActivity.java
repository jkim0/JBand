package com.itj.jband;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Random;

public class ActivityDetailActivity extends AppCompatActivity {
    private static final String TAG = ActivityDetailActivity.class.getSimpleName();

    private XYMultipleSeriesRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initRenderer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initRenderer() {
        mRenderer = new XYMultipleSeriesRenderer();

        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(Color.BLUE);
        mRenderer.addSeriesRenderer(r);

        mRenderer.setXAxisMin(0.5);
        mRenderer.setXAxisMax(10.5);
        mRenderer.setYAxisMin(0);
        mRenderer.setYAxisMax(210);
        mRenderer.setMargins(new int[]{30, 0, 0, 0});
        mRenderer.setPanEnabled(true, false);
        mRenderer.setBarSpacing(0.1f);
        mRenderer.setShowLegend(false);
        mRenderer.setAxesColor(Color.BLACK);
        mRenderer.setLabelsColor(Color.WHITE);
        mRenderer.setLabelsTextSize(30);
        mRenderer.setMarginsColor(android.R.color.background_light);

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        final int nr = 10;
        Random random = new Random();

        CategorySeries series = new CategorySeries("Demo series 1");
        for (int k = 0; k < nr; k++) {
            series.add(100 + random.nextInt() % 100);
        }
        dataset.addSeries(series.toXYSeries());

        GraphicalView view = ChartFactory.getBarChartView(this, dataset, mRenderer, BarChart.Type.DEFAULT);

        LinearLayout container = (LinearLayout)findViewById(R.id.chart_container);
        container.addView(view);
    }
}
