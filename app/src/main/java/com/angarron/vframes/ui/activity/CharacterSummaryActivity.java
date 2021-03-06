package com.angarron.vframes.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.angarron.vframes.BuildConfig;
import com.angarron.vframes.R;
import com.angarron.vframes.adapter.SummaryPagerAdapter;
import com.angarron.vframes.application.VFramesApplication;
import com.angarron.vframes.ui.fragment.MoveListFragment;
import com.angarron.vframes.ui.fragment.NotesFragment;
import com.angarron.vframes.ui.fragment.GuideVideosFragment;
import com.angarron.vframes.ui.fragment.TournamentVideosFragment;
import com.angarron.vframes.util.CharacterResourceUtil;
import com.angarron.vframes.util.CrashlyticsUtil;
import com.angarron.vframes.util.FeedbackUtil;
import com.crashlytics.android.Crashlytics;

import java.util.List;
import java.util.Map;

import data.model.CharacterID;
import data.model.IDataModel;
import data.model.character.SFCharacter;
import data.model.move.IMoveListEntry;
import data.model.move.MoveCategory;

public class CharacterSummaryActivity extends AppCompatActivity implements
        MoveListFragment.IMoveListFragmentHost,
        GuideVideosFragment.IGuideVideosFragmentHost,
        TournamentVideosFragment.ITournamentVideosFragmentHost,
        NotesFragment.INotesFragmentHost,
        AdapterView.OnItemSelectedListener, ViewPager.OnPageChangeListener {

    public static final String INTENT_EXTRA_TARGET_CHARACTER = "INTENT_EXTRA_TARGET_CHARACTER";

    private static final int NOTES_ACTIVITY_REQUEST_CODE = 1;

    private CharacterID targetCharacter;

    private ViewPager viewPager;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_character_summary);

        //Postpone the transition to give the header image time to get laid out.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }

        try {
            targetCharacter = (CharacterID) getIntent().getSerializableExtra(INTENT_EXTRA_TARGET_CHARACTER);
        } catch (ClassCastException e) {
            Crashlytics.log(Log.ERROR, VFramesApplication.APP_LOGGING_TAG, "failed to parse intent in CharacterSummaryActivity");
            finish();
        }

        //Verify the data is still available. If not, send to splash screen.
        if (dataIsAvailable()) {
            //Load the toolbar based on the target character
            setupToolbar();
            setCharacterDetails();
            setupViewPager();
            setupSpinner();
        } else {
            sendToSplashScreen();
        }
    }

    private void setupSpinner() {
        spinner = (Spinner) findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.summary_spinner_options, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (dataIsAvailable()) {
            return super.onPrepareOptionsMenu(menu);
        } else {
            sendToSplashScreen();
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_summary, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_feedback:
                FeedbackUtil.sendFeedback(this);
                return true;
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            default:
                throw new RuntimeException("invalid menu item clicked");
        }
    }

    //Move List Fragment Host
    @Override
    public Map<MoveCategory, List<IMoveListEntry>> getMoveList() {
        VFramesApplication application = (VFramesApplication) getApplication();
        IDataModel dataModel = application.getDataModel();
        SFCharacter targetCharacterModel = dataModel.getCharactersModel().getCharacter(targetCharacter);
        return targetCharacterModel.getMoveList();
    }

    @Override
    public void onVideoSelected(String videoUrl) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(videoUrl));
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }

    private boolean dataIsAvailable() {
        VFramesApplication application = (VFramesApplication) getApplication();
        return (application.getDataModel() != null);
    }

    private void setupViewPager() {
        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new SummaryPagerAdapter(this, getSupportFragmentManager(), targetCharacter);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(this);

        PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        pagerTabStrip.setBackgroundColor(getCharacterAccentColor());
        pagerTabStrip.setTabIndicatorColorResource(R.color.tab_indicator_color);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.summary_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getCharacterAccentColor());
        }

        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            actionBar.setTitle(CharacterResourceUtil.getCharacterDisplayName(this, targetCharacter));
            actionBar.setBackgroundDrawable(CharacterResourceUtil.getCharacterPrimaryColorDrawable(this, targetCharacter));

            if (viewExists(R.id.summary_character_image)) {
                final ImageView summaryCharacterImage = (ImageView) findViewById(R.id.summary_character_image);
                ViewTreeObserver viewTreeObserver = summaryCharacterImage.getViewTreeObserver();
                viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        summaryCharacterImage.getViewTreeObserver().removeOnPreDrawListener(this);
                        finishEnterTransition();
                        return true;
                    }
                });
                summaryCharacterImage.setImageResource(CharacterResourceUtil.getCharacterBannerResource(targetCharacter));
            } else {
                //Even though there is no header image, we still need to call startPostponedEnterTransition()
                //to finish transitioning to this activity.
                finishEnterTransition();
            }
        }
    }

    private void finishEnterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startPostponedEnterTransition();
        }
    }

    private void sendToSplashScreen() {
        //If this is a release build, log this issue to Crashlytics.
        if (!BuildConfig.DEBUG) {
            Crashlytics.logException(new Throwable("Sending user to splash screen because data was unavailable"));
        }

        Intent startSplashIntent = new Intent(this, SplashActivity.class);
        startActivity(startSplashIntent);
        finish();
    }

    private boolean viewExists(int viewId) {
        return findViewById(viewId) != null;
    }

    private int getCharacterAccentColor() {
        switch(targetCharacter) {
            case JURI:
                return ContextCompat.getColor(this, R.color.juri_accent);
            case BOXER:
                return ContextCompat.getColor(this, R.color.boxer_accent);
            case IBUKI:
                return ContextCompat.getColor(this, R.color.ibuki_accent);
            case GUILE:
                return ContextCompat.getColor(this, R.color.guile_accent);
            case ALEX:
                return ContextCompat.getColor(this, R.color.alex_accent);
            case RYU:
                return ContextCompat.getColor(this, R.color.ryu_accent);
            case CHUN:
                return ContextCompat.getColor(this, R.color.chun_accent);
            case DICTATOR:
                return ContextCompat.getColor(this, R.color.dictator_accent);
            case BIRDIE:
                return ContextCompat.getColor(this, R.color.birdie_accent);
            case NASH:
                return ContextCompat.getColor(this, R.color.nash_accent);
            case CAMMY:
                return ContextCompat.getColor(this, R.color.cammy_accent);
            case KEN:
                return ContextCompat.getColor(this, R.color.ken_accent);
            case MIKA:
                return ContextCompat.getColor(this, R.color.mika_accent);
            case NECALLI:
                return ContextCompat.getColor(this, R.color.necalli_accent);
            case CLAW:
                return ContextCompat.getColor(this, R.color.claw_accent);
            case RASHID:
                return ContextCompat.getColor(this, R.color.rashid_accent);
            case KARIN:
                return ContextCompat.getColor(this, R.color.karin_accent);
            case LAURA:
                return ContextCompat.getColor(this, R.color.laura_accent);
            case DHALSIM:
                return ContextCompat.getColor(this, R.color.dhalsim_accent);
            case ZANGIEF:
                return ContextCompat.getColor(this, R.color.zangief_accent);
            case FANG:
                return ContextCompat.getColor(this, R.color.fang_accent);
            default:
                throw new RuntimeException("unable to resolve character accent color drawable: " + targetCharacter);
        }
    }

    private void setCharacterDetails() {
        int titleStringId;
        int healthStringId;
        int stunStringId;

        switch (targetCharacter) {
            case JURI:
                titleStringId = R.string.juri_title;
                healthStringId = R.string.juri_health;
                stunStringId = R.string.juri_stun;
                break;
            case IBUKI:
                titleStringId = R.string.ibuki_title;
                healthStringId = R.string.ibuki_health;
                stunStringId = R.string.ibuki_stun;
                break;
            case BOXER:
                titleStringId = R.string.boxer_title;
                healthStringId = R.string.boxer_health;
                stunStringId = R.string.boxer_stun;
                break;
            case GUILE:
                titleStringId = R.string.guile_title;
                healthStringId = R.string.guile_health;
                stunStringId = R.string.guile_stun;
                break;
            case ALEX:
                titleStringId = R.string.alex_title;
                healthStringId = R.string.alex_health;
                stunStringId = R.string.alex_stun;
                break;
            case RYU:
                titleStringId = R.string.ryu_title;
                healthStringId = R.string.ryu_health;
                stunStringId = R.string.ryu_stun;
                break;
            case CHUN:
                titleStringId = R.string.chun_title;
                healthStringId = R.string.chun_health;
                stunStringId = R.string.chun_stun;
                break;
            case DICTATOR:
                titleStringId = R.string.dictator_title;
                healthStringId = R.string.dictator_health;
                stunStringId = R.string.dictator_stun;
                break;
            case BIRDIE:
                titleStringId = R.string.birdie_title;
                healthStringId = R.string.birdie_health;
                stunStringId = R.string.birdie_stun;
                break;
            case NASH:
                titleStringId = R.string.nash_title;
                healthStringId = R.string.nash_health;
                stunStringId = R.string.nash_stun;
                break;
            case CAMMY:
                titleStringId = R.string.cammy_title;
                healthStringId = R.string.cammy_health;
                stunStringId = R.string.cammy_stun;
                break;
            case CLAW:
                titleStringId = R.string.claw_title;
                healthStringId = R.string.claw_health;
                stunStringId = R.string.claw_stun;
                break;
            case LAURA:
                titleStringId = R.string.laura_title;
                healthStringId = R.string.laura_health;
                stunStringId = R.string.laura_stun;
                break;
            case KEN:
                titleStringId = R.string.ken_title;
                healthStringId = R.string.ken_health;
                stunStringId = R.string.ken_stun;
                break;
            case NECALLI:
                titleStringId = R.string.necalli_title;
                healthStringId = R.string.necalli_health;
                stunStringId = R.string.necalli_stun;
                break;
            case RASHID:
                titleStringId = R.string.rashid_title;
                healthStringId = R.string.rashid_health;
                stunStringId = R.string.rashid_stun;
                break;
            case MIKA:
                titleStringId = R.string.mika_title;
                healthStringId = R.string.mika_health;
                stunStringId = R.string.mika_stun;
                break;
            case ZANGIEF:
                titleStringId = R.string.zangief_title;
                healthStringId = R.string.zangief_health;
                stunStringId = R.string.zangief_stun;
                break;
            case FANG:
                titleStringId = R.string.fang_title;
                healthStringId = R.string.fang_health;
                stunStringId = R.string.fang_stun;
                break;
            case DHALSIM:
                titleStringId = R.string.dhalsim_title;
                healthStringId = R.string.dhalsim_health;
                stunStringId = R.string.dhalsim_stun;
                break;
            case KARIN:
                titleStringId = R.string.karin_title;
                healthStringId = R.string.karin_health;
                stunStringId = R.string.karin_stun;
                break;
            default:
                throw new IllegalArgumentException("could not find character: " + targetCharacter.toString());
        }

        if (viewExists(R.id.banner_character_details)) {
            ((TextView) findViewById(R.id.banner_character_title)).setText(titleStringId);

            String healthText = String.format(getString(R.string.health_format), getString(healthStringId));
            ((TextView) findViewById(R.id.banner_character_health)).setText(healthText);

            String stunText = String.format(getString(R.string.stun_format), getString(stunStringId));
            ((TextView) findViewById(R.id.banner_character_stun)).setText(stunText);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        viewPager.setCurrentItem(i, true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        //no-op
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //no-op
    }

    @Override
    public void onPageSelected(int position) {
        spinner.setSelection(position, true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //no-op
    }

    @Override
    public void onGeneralNotesSelected() {
        Intent intent = new Intent(this, NotesActivity.class);
        intent.putExtra(NotesActivity.INTENT_EXTRA_NOTES_TYPE, NotesActivity.NOTES_TYPE_CHARACTER_GENERAL);
        intent.putExtra(NotesActivity.INTENT_EXTRA_CHARACTER, targetCharacter);
        startActivityForResult(intent, NOTES_ACTIVITY_REQUEST_CODE);
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still);
    }

    @Override
    public void onMatchupNotesSelected(CharacterID secondCharacter) {
        Intent intent = new Intent(this, NotesActivity.class);
        intent.putExtra(NotesActivity.INTENT_EXTRA_NOTES_TYPE, NotesActivity.NOTES_TYPE_MATCHUP);
        intent.putExtra(NotesActivity.INTENT_EXTRA_CHARACTER, targetCharacter);
        intent.putExtra(NotesActivity.INTENT_EXTRA_SECOND_CHARACTER, secondCharacter);
        startActivityForResult(intent, NOTES_ACTIVITY_REQUEST_CODE);
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NOTES_ACTIVITY_REQUEST_CODE) {
            boolean didSaveNotes = data.getBooleanExtra(NotesActivity.DID_SAVE_NOTES, false);
            CrashlyticsUtil.sendViewedNotesEvent(didSaveNotes);
        }
    }
}
