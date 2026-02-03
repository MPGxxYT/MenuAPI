# MenuAPI - Testing Guide

## Test Setup

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>

    <!-- MockBukkit - Required for most MenuAPI tests -->
    <dependency>
        <groupId>com.github.seeseemelk</groupId>
        <artifactId>MockBukkit-v1.20</artifactId>
        <version>3.80.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

### Test Directory Structure

```
src/
├── main/java/...
└── test/java/me/mortaldev/menuapi/
    ├── GUIManagerTest.java
    ├── InventoryGUITest.java
    ├── InventoryButtonTest.java
    ├── IAButtonTest.java
    ├── AutoRefreshTest.java
    ├── GlobalGUIManagementTest.java
    └── testutil/
        ├── TestMenu.java
        ├── TestPlugin.java
        └── MockHelper.java
```

---

## Automated Tests (JUnit/Mockito + MockBukkit)

> **Note:** MenuAPI is heavily Bukkit-dependent. Most tests require MockBukkit to simulate players, inventories, and events.

### 1. InventoryButtonTest.java

```java
class InventoryButtonTest {

    // --- Fluent API ---

    @Test
    void creator_setsIconCreator()

    @Test
    void consumer_setsEventConsumer()

    @Test
    void creator_chainsWithConsumer()

    // --- Icon Creation ---

    @Test
    void getIcon_callsCreatorWithPlayer()

    @Test
    void getIcon_withNullCreator_returnsNull()

    // --- Click Handling ---

    @Test
    void handleClick_callsConsumer()

    @Test
    void handleClick_withNullConsumer_noException()

    @Test
    void handleClick_passesCorrectEvent()
}
```

### 2. IAButtonTest.java

```java
class IAButtonTest {

    // --- Size Configuration ---

    @Test
    void size_setsWidthAndHeight()

    @Test
    void size_width1_valid()

    @Test
    void size_width9_valid()

    @Test
    void size_width0_throwsException()

    @Test
    void size_width10_throwsException()

    @Test
    void size_height1_valid()

    @Test
    void size_height6_valid()

    @Test
    void size_height0_throwsException()

    @Test
    void size_height7_throwsException()

    // --- Anchor Configuration ---

    @Test
    void anchor_setsAnchorSlot()

    @Test
    void anchor_slot0_valid()

    @Test
    void anchor_slot53_valid()

    @Test
    void anchor_negativeSlot_throwsException()

    // --- Slot Calculation ---

    @Test
    void getSlots_1x1_returnsSingleSlot()

    @Test
    void getSlots_3x2_returns6Slots()

    @Test
    void getSlots_9x1_returnsFullRow()

    @Test
    void getSlots_1x6_returnsFullColumn()

    @Test
    void getSlots_anchorAtSlot10_correctSlots()

    @Test
    void getSlots_wrapsToNextRow_correctly()

    // --- Boundary Cases ---

    @Test
    void getSlots_atRightEdge_staysInBounds()

    @Test
    void getSlots_atBottomEdge_staysInBounds()

    // --- Fluent API ---

    @Test
    void fluentChain_sizeAnchorCreatorConsumer_works()
}
```

### 3. InventoryGUITest.java (MockBukkit Required)

```java
@ExtendWith(MockBukkitExtension.class)
class InventoryGUITest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- Inventory Creation ---

    @Test
    void createInventory_returnsCorrectSize()

    @Test
    void createInventory_returnsCorrectTitle()

    // --- Player Registration ---

    @Test
    void registerPlayer_setsPlayer()

    @Test
    void registerPlayer_calledTwice_throwsException()

    @Test
    void getRegisteredPlayer_beforeRegister_throwsException()

    @Test
    void getRegisteredPlayer_afterRegister_returnsPlayer()

    // --- Button Management ---

    @Test
    void addButton_singleSlot_registersButton()

    @Test
    void addButton_multipleSlots_registersInAll()

    @Test
    void addButton_iaButton_registersInArea()

    // --- Decoration ---

    @Test
    void decorate_placesButtonsInInventory()

    @Test
    void decorate_calledMultipleTimes_noErrors()

    // --- Click Control ---

    @Test
    void allowTopInventoryClick_default_isFalse()

    @Test
    void allowBottomInventoryClick_default_isFalse()

    @Test
    void allowTopInventoryClick_true_allowsClicks()

    @Test
    void allowBottomInventoryClick_true_allowsClicks()

    // --- Button Refresh ---

    @Test
    void refreshButton_updatesSlotItem()

    @Test
    void refreshButtons_updatesAllSlots()

    @Test
    void refreshButton_iaButton_updatesAllSlots()

    // --- Static Tracking ---

    @Test
    void hasOpenGUI_withOpenGUI_returnsTrue()

    @Test
    void hasOpenGUI_withClosedGUI_returnsFalse()

    @Test
    void getOpenGUI_withOpenGUI_returnsInstance()

    @Test
    void getOpenGUI_withNoGUI_returnsNull()
}
```

### 4. GUIManagerTest.java (MockBukkit Required)

```java
@ExtendWith(MockBukkitExtension.class)
class GUIManagerTest {

    private ServerMock server;
    private PlayerMock player;

    // --- Singleton ---

    @Test
    void getInstance_returnsSameInstance()

    // --- Opening GUIs ---

    @Test
    void openGUI_opensInventoryForPlayer()

    @Test
    void openGUI_registersWithManager()

    @Test
    void openGUI_callsDecorate()

    @Test
    void openGUI_triggersOnOpen()

    // --- Closing GUIs ---

    @Test
    void closeGUI_unregistersFromManager()

    @Test
    void closeGUI_triggersOnClose()

    // --- Click Handling ---

    @Test
    void handleClick_dispatchesToCorrectButton()

    @Test
    void handleClick_cancelsEventWhenDisallowed()

    @Test
    void handleClick_allowsEventWhenAllowed()

    // --- Multiple Players ---

    @Test
    void openGUI_multiplePlayerssameSameType_works()

    @Test
    void openGUI_differentGUITypes_works()

    // --- GUI Replacement ---

    @Test
    void openGUI_replacesExistingGUI()

    @Test
    void openGUI_closesOldGUIFirst()
}
```

### 5. AutoRefreshTest.java (MockBukkit Required)

```java
@ExtendWith(MockBukkitExtension.class)
class AutoRefreshTest {

    private ServerMock server;
    private JavaPluginMock plugin;
    private PlayerMock player;

    // --- Enable/Disable ---

    @Test
    void enableAutoRefresh_validInterval_succeeds()

    @Test
    void enableAutoRefresh_zeroInterval_throwsException()

    @Test
    void enableAutoRefresh_negativeInterval_throwsException()

    // --- Task Lifecycle ---

    @Test
    void autoRefresh_startsOnOpen()

    @Test
    void autoRefresh_stopsOnClose()

    @Test
    void autoRefresh_noLeakedTasks()

    // --- Refresh Behavior ---

    @Test
    void autoRefresh_callsRefreshGUI()

    @Test
    void autoRefresh_updatesButtonIcons()

    // --- Player Disconnect ---

    @Test
    void autoRefresh_stopsOnDisconnect()

    // --- Interval Timing ---

    @Test
    void autoRefresh_20ticks_refreshesEverySecond()
}
```

### 6. GlobalGUIManagementTest.java (MockBukkit Required)

```java
@ExtendWith(MockBukkitExtension.class)
class GlobalGUIManagementTest {

    private ServerMock server;
    private PlayerMock player1;
    private PlayerMock player2;

    // --- Type-Based Refresh ---

    @Test
    void refreshAllGUIsOfType_refreshesMatchingType()

    @Test
    void refreshAllGUIsOfType_ignoresOtherTypes()

    @Test
    void refreshButtonsForType_refreshesButtonsOnly()

    // --- Multiple Players ---

    @Test
    void refreshAllGUIsOfType_affectsAllPlayers()

    @Test
    void refreshButtonsForType_affectsAllPlayers()

    // --- No Open GUIs ---

    @Test
    void refreshAllGUIsOfType_noOpenGUIs_noErrors()
}
```

### 7. EventHandlingTest.java (MockBukkit Required)

```java
@ExtendWith(MockBukkitExtension.class)
class EventHandlingTest {

    // --- Click Events ---

    @Test
    void onClick_calledBeforeButtonHandler()

    @Test
    void onClick_canCancelEvent()

    // --- Open Events ---

    @Test
    void onOpen_calledWhenOpened()

    @Test
    void onOpen_receivesCorrectEvent()

    // --- Close Events ---

    @Test
    void onClose_calledWhenClosed()

    @Test
    void onClose_receivesCorrectEvent()

    // --- Drag Events ---

    @Test
    void onDrag_handledCorrectly()
}
```

---

## Manual Tests (Require Paper Server)

These tests cannot be reliably automated because they require:
- Real inventory rendering
- Actual player interaction
- ItemsAdder plugin integration
- Visual verification
- Performance under load

### Basic Functionality

- [ ] Open menu - displays correctly
- [ ] Click button - action triggers
- [ ] Close with Escape - closes cleanly
- [ ] Close by clicking outside - closes cleanly

### Visual Verification

- [ ] Button icons display correct items
- [ ] Button icons display correct names
- [ ] Button icons display correct lore
- [ ] Inventory title displays correctly

### InventoryButton

- [ ] Single slot button works
- [ ] Multi-slot button (same button in multiple slots) works
- [ ] Dynamic icons update based on player data
- [ ] Click feedback (sounds, messages) works

### IAButton (Area Buttons)

- [ ] Area button fills correct slots
- [ ] Click on any slot in area triggers handler
- [ ] Visual consistency across area slots
- [ ] Refresh updates all slots in area

### Auto-Refresh

- [ ] Content updates at correct interval
- [ ] No visual flickering during refresh
- [ ] Task stops when menu closes
- [ ] No task leaks (check `/paper timings`)
- [ ] Multiple menus with different intervals work

### Click Control

- [ ] Top inventory click blocking works
- [ ] Bottom inventory click blocking works
- [ ] Shift-click handling correct
- [ ] Number key handling correct
- [ ] Double-click handling correct

### Multiple Players

- [ ] Two players can open same menu type
- [ ] Each player sees their own data
- [ ] Global refresh updates both players
- [ ] One player closing doesn't affect other

### Player State

- [ ] Menu works in different worlds
- [ ] Menu handles player teleport
- [ ] Menu handles player death
- [ ] Player logout cleans up properly

### ItemsAdder Integration (Optional)

Skip if ItemsAdder not installed.

- [ ] TexturedInventoryGUI displays texture
- [ ] HUD shows when menu opens
- [ ] HUD hides when menu closes
- [ ] Custom item models display correctly

### Edge Cases

- [ ] Rapid open/close - no errors
- [ ] Rapid clicking - no errors
- [ ] Very fast refresh (1 tick) - stable
- [ ] Many buttons (54 slots full) - works
- [ ] Empty menu (no buttons) - works

### Performance

- [ ] Large menus open quickly
- [ ] Auto-refresh doesn't cause lag
- [ ] Many open menus don't impact TPS
- [ ] No memory leaks over time

### Inventory Sizes

- [ ] 9-slot (1 row) works
- [ ] 27-slot (3 rows) works
- [ ] 54-slot (6 rows) works
- [ ] IAButton respects boundaries in all sizes

---

## Test Utilities

### TestMenu.java

```java
public class TestMenu extends InventoryGUI {

    private final String title;
    private final int size;

    public TestMenu(String title, int size) {
        this.title = title;
        this.size = size;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, size, title);
    }

    @Override
    public void decorate(Player player) {
        addButton(0, new InventoryButton()
            .creator(p -> new ItemStack(Material.DIAMOND))
            .consumer(e -> e.setCancelled(true)));
        super.decorate(player);
    }
}
```

### TestPlugin.java

```java
public class TestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        GUIListener listener = new GUIListener(GUIManager.getInstance());
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
```

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=InventoryGUITest

# Run with coverage
mvn test jacoco:report

# Run only unit tests (no MockBukkit)
mvn test -Dtest=InventoryButtonTest,IAButtonTest
```

---

## Version Checklist

Before tagging a release:

- [ ] All automated tests pass (`mvn test`)
- [ ] Manual server tests pass
- [ ] No compiler warnings
- [ ] README.md version updated
- [ ] Test with latest Paper version
- [ ] Test with ItemsAdder (if applicable)
