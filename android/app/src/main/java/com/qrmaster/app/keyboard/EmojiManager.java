package com.qrmaster.app.keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmojiManager {
    
    public enum Category {
        SMILEYS("😊", "Gülümsemeler"),
        GESTURES("👋", "Jestler"),
        PEOPLE("👨", "İnsanlar"),
        ANIMALS("🐶", "Hayvanlar"),
        FOOD("🍕", "Yiyecek"),
        TRAVEL("✈️", "Seyahat"),
        ACTIVITIES("⚽", "Aktiviteler"),
        OBJECTS("💡", "Objeler"),
        SYMBOLS("❤️", "Semboller"),
        FLAGS("🇹🇷", "Bayraklar");
        
        private final String icon;
        private final String name;
        
        Category(String icon, String name) {
            this.icon = icon;
            this.name = name;
        }
        
        public String getIcon() { return icon; }
        public String getName() { return name; }
    }
    
    private static final Map<Category, List<String>> EMOJI_MAP = new HashMap<>();
    
    static {
        // Smileys & Emotion
        List<String> smileys = new ArrayList<>();
        smileys.add("😀"); smileys.add("😃"); smileys.add("😄"); smileys.add("😁");
        smileys.add("😆"); smileys.add("😅"); smileys.add("🤣"); smileys.add("😂");
        smileys.add("🙂"); smileys.add("🙃"); smileys.add("😉"); smileys.add("😊");
        smileys.add("😇"); smileys.add("🥰"); smileys.add("😍"); smileys.add("🤩");
        smileys.add("😘"); smileys.add("😗"); smileys.add("😚"); smileys.add("😙");
        smileys.add("😋"); smileys.add("😛"); smileys.add("😜"); smileys.add("🤪");
        smileys.add("😝"); smileys.add("🤑"); smileys.add("🤗"); smileys.add("🤭");
        smileys.add("🤫"); smileys.add("🤔"); smileys.add("🤐"); smileys.add("🤨");
        smileys.add("😐"); smileys.add("😑"); smileys.add("😶"); smileys.add("😏");
        smileys.add("😒"); smileys.add("🙄"); smileys.add("😬"); smileys.add("🤥");
        smileys.add("😌"); smileys.add("😔"); smileys.add("😪"); smileys.add("🤤");
        smileys.add("😴"); smileys.add("😷"); smileys.add("🤒"); smileys.add("🤕");
        smileys.add("🤢"); smileys.add("🤮"); smileys.add("🤧"); smileys.add("🥵");
        smileys.add("🥶"); smileys.add("🥴"); smileys.add("😵"); smileys.add("🤯");
        smileys.add("🤠"); smileys.add("🥳"); smileys.add("😎"); smileys.add("🤓");
        smileys.add("🧐"); smileys.add("😕"); smileys.add("😟"); smileys.add("🙁");
        smileys.add("☹️"); smileys.add("😮"); smileys.add("😯"); smileys.add("😲");
        smileys.add("😳"); smileys.add("🥺"); smileys.add("😦"); smileys.add("😧");
        smileys.add("😨"); smileys.add("😰"); smileys.add("😥"); smileys.add("😢");
        smileys.add("😭"); smileys.add("😱"); smileys.add("😖"); smileys.add("😣");
        smileys.add("😞"); smileys.add("😓"); smileys.add("😩"); smileys.add("😫");
        smileys.add("🥱"); smileys.add("😤"); smileys.add("😡"); smileys.add("😠");
        smileys.add("🤬"); smileys.add("😈"); smileys.add("👿"); smileys.add("💀");
        smileys.add("☠️"); smileys.add("💩"); smileys.add("🤡"); smileys.add("👹");
        smileys.add("👺"); smileys.add("👻"); smileys.add("👽"); smileys.add("👾");
        smileys.add("🤖"); smileys.add("😺"); smileys.add("😸"); smileys.add("😹");
        smileys.add("😻"); smileys.add("😼"); smileys.add("😽"); smileys.add("🙀");
        smileys.add("😿"); smileys.add("😾");
        EMOJI_MAP.put(Category.SMILEYS, smileys);
        
        // Gestures & Body Parts
        List<String> gestures = new ArrayList<>();
        gestures.add("👋"); gestures.add("🤚"); gestures.add("🖐️"); gestures.add("✋");
        gestures.add("🖖"); gestures.add("👌"); gestures.add("🤏"); gestures.add("✌️");
        gestures.add("🤞"); gestures.add("🤟"); gestures.add("🤘"); gestures.add("🤙");
        gestures.add("👈"); gestures.add("👉"); gestures.add("👆"); gestures.add("🖕");
        gestures.add("👇"); gestures.add("☝️"); gestures.add("👍"); gestures.add("👎");
        gestures.add("✊"); gestures.add("👊"); gestures.add("🤛"); gestures.add("🤜");
        gestures.add("👏"); gestures.add("🙌"); gestures.add("👐"); gestures.add("🤲");
        gestures.add("🤝"); gestures.add("🙏"); gestures.add("✍️"); gestures.add("💅");
        gestures.add("🤳"); gestures.add("💪"); gestures.add("🦾"); gestures.add("🦿");
        gestures.add("🦵"); gestures.add("🦶"); gestures.add("👂"); gestures.add("🦻");
        gestures.add("👃"); gestures.add("🧠"); gestures.add("🦷"); gestures.add("🦴");
        gestures.add("👀"); gestures.add("👁️"); gestures.add("👅"); gestures.add("👄");
        EMOJI_MAP.put(Category.GESTURES, gestures);
        
        // People & Fantasy
        List<String> people = new ArrayList<>();
        people.add("👶"); people.add("👧"); people.add("🧒"); people.add("👦");
        people.add("👩"); people.add("🧑"); people.add("👨"); people.add("👩‍🦱");
        people.add("🧑‍🦱"); people.add("👨‍🦱"); people.add("👩‍🦰"); people.add("🧑‍🦰");
        people.add("👨‍🦰"); people.add("👱‍♀️"); people.add("👱"); people.add("👱‍♂️");
        people.add("👩‍🦳"); people.add("🧑‍🦳"); people.add("👨‍🦳"); people.add("👩‍🦲");
        people.add("🧑‍🦲"); people.add("👨‍🦲"); people.add("🧔"); people.add("👵");
        people.add("🧓"); people.add("👴"); people.add("👲"); people.add("👳‍♀️");
        people.add("👳"); people.add("👳‍♂️"); people.add("🧕"); people.add("👮‍♀️");
        people.add("👮"); people.add("👮‍♂️"); people.add("👷‍♀️"); people.add("👷");
        people.add("👷‍♂️"); people.add("💂‍♀️"); people.add("💂"); people.add("💂‍♂️");
        EMOJI_MAP.put(Category.PEOPLE, people);
        
        // Animals & Nature
        List<String> animals = new ArrayList<>();
        animals.add("🐶"); animals.add("🐱"); animals.add("🐭"); animals.add("🐹");
        animals.add("🐰"); animals.add("🦊"); animals.add("🐻"); animals.add("🐼");
        animals.add("🐨"); animals.add("🐯"); animals.add("🦁"); animals.add("🐮");
        animals.add("🐷"); animals.add("🐽"); animals.add("🐸"); animals.add("🐵");
        animals.add("🙈"); animals.add("🙉"); animals.add("🙊"); animals.add("🐒");
        animals.add("🐔"); animals.add("🐧"); animals.add("🐦"); animals.add("🐤");
        animals.add("🐣"); animals.add("🐥"); animals.add("🦆"); animals.add("🦅");
        animals.add("🦉"); animals.add("🦇"); animals.add("🐺"); animals.add("🐗");
        animals.add("🐴"); animals.add("🦄"); animals.add("🐝"); animals.add("🐛");
        animals.add("🦋"); animals.add("🐌"); animals.add("🐞"); animals.add("🐜");
        animals.add("🦟"); animals.add("🦗"); animals.add("🕷️"); animals.add("🦂");
        animals.add("🐢"); animals.add("🐍"); animals.add("🦎"); animals.add("🦖");
        animals.add("🦕"); animals.add("🐙"); animals.add("🦑"); animals.add("🦐");
        animals.add("🦞"); animals.add("🦀"); animals.add("🐡"); animals.add("🐠");
        animals.add("🐟"); animals.add("🐬"); animals.add("🐳"); animals.add("🐋");
        animals.add("🦈"); animals.add("🐊"); animals.add("🐅"); animals.add("🐆");
        EMOJI_MAP.put(Category.ANIMALS, animals);
        
        // Food & Drink
        List<String> food = new ArrayList<>();
        food.add("🍇"); food.add("🍈"); food.add("🍉"); food.add("🍊");
        food.add("🍋"); food.add("🍌"); food.add("🍍"); food.add("🥭");
        food.add("🍎"); food.add("🍏"); food.add("🍐"); food.add("🍑");
        food.add("🍒"); food.add("🍓"); food.add("🥝"); food.add("🍅");
        food.add("🥥"); food.add("🥑"); food.add("🍆"); food.add("🥔");
        food.add("🥕"); food.add("🌽"); food.add("🌶️"); food.add("🥒");
        food.add("🥬"); food.add("🥦"); food.add("🧄"); food.add("🧅");
        food.add("🍄"); food.add("🥜"); food.add("🌰"); food.add("🍞");
        food.add("🥐"); food.add("🥖"); food.add("🥨"); food.add("🥯");
        food.add("🥞"); food.add("🧇"); food.add("🧀"); food.add("🍖");
        food.add("🍗"); food.add("🥩"); food.add("🥓"); food.add("🍔");
        food.add("🍟"); food.add("🍕"); food.add("🌭"); food.add("🥪");
        food.add("🌮"); food.add("🌯"); food.add("🥙"); food.add("🧆");
        food.add("🥚"); food.add("🍳"); food.add("🥘"); food.add("🍲");
        food.add("🥣"); food.add("🥗"); food.add("🍿"); food.add("🧈");
        food.add("🧂"); food.add("🥫"); food.add("🍱"); food.add("🍘");
        food.add("🍙"); food.add("🍚"); food.add("🍛"); food.add("🍜");
        food.add("🍝"); food.add("🍠"); food.add("🍢"); food.add("🍣");
        food.add("🍤"); food.add("🍥"); food.add("🥮"); food.add("🍡");
        food.add("🥟"); food.add("🥠"); food.add("🥡"); food.add("🦀");
        food.add("🦞"); food.add("🦐"); food.add("🦑"); food.add("🦪");
        food.add("🍦"); food.add("🍧"); food.add("🍨"); food.add("🍩");
        food.add("🍪"); food.add("🎂"); food.add("🍰"); food.add("🧁");
        food.add("🥧"); food.add("🍫"); food.add("🍬"); food.add("🍭");
        food.add("🍮"); food.add("🍯"); food.add("🍼"); food.add("🥛");
        food.add("☕"); food.add("🍵"); food.add("🍶"); food.add("🍾");
        food.add("🍷"); food.add("🍸"); food.add("🍹"); food.add("🍺");
        food.add("🍻"); food.add("🥂"); food.add("🥃"); food.add("🥤");
        food.add("🧃"); food.add("🧉"); food.add("🧊");
        EMOJI_MAP.put(Category.FOOD, food);
        
        // Travel & Places
        List<String> travel = new ArrayList<>();
        travel.add("🚗"); travel.add("🚕"); travel.add("🚙"); travel.add("🚌");
        travel.add("🚎"); travel.add("🏎️"); travel.add("🚓"); travel.add("🚑");
        travel.add("🚒"); travel.add("🚐"); travel.add("🚚"); travel.add("🚛");
        travel.add("🚜"); travel.add("🛴"); travel.add("🚲"); travel.add("🛵");
        travel.add("🏍️"); travel.add("🛺"); travel.add("🚨"); travel.add("🚔");
        travel.add("🚍"); travel.add("🚘"); travel.add("🚖"); travel.add("🚡");
        travel.add("🚠"); travel.add("🚟"); travel.add("🚃"); travel.add("🚋");
        travel.add("🚞"); travel.add("🚝"); travel.add("🚄"); travel.add("🚅");
        travel.add("🚈"); travel.add("🚂"); travel.add("🚆"); travel.add("🚇");
        travel.add("🚊"); travel.add("🚉"); travel.add("✈️"); travel.add("🛫");
        travel.add("🛬"); travel.add("🛩️"); travel.add("💺"); travel.add("🛰️");
        travel.add("🚀"); travel.add("🛸"); travel.add("🚁"); travel.add("🛶");
        travel.add("⛵"); travel.add("🚤"); travel.add("🛥️"); travel.add("🛳️");
        travel.add("⛴️"); travel.add("🚢"); travel.add("⚓"); travel.add("⛽");
        travel.add("🚧"); travel.add("🚦"); travel.add("🚥"); travel.add("🗺️");
        travel.add("🗿"); travel.add("🗽"); travel.add("🗼"); travel.add("🏰");
        travel.add("🏯"); travel.add("🏟️"); travel.add("🎡"); travel.add("🎢");
        travel.add("🎠"); travel.add("⛲"); travel.add("⛱️"); travel.add("🏖️");
        travel.add("🏝️"); travel.add("🏜️"); travel.add("🌋"); travel.add("⛰️");
        travel.add("🏔️"); travel.add("🗻"); travel.add("🏕️"); travel.add("⛺");
        travel.add("🏠"); travel.add("🏡"); travel.add("🏘️"); travel.add("🏚️");
        travel.add("🏗️"); travel.add("🏭"); travel.add("🏢"); travel.add("🏬");
        travel.add("🏣"); travel.add("🏤"); travel.add("🏥"); travel.add("🏦");
        travel.add("🏨"); travel.add("🏪"); travel.add("🏫"); travel.add("🏩");
        travel.add("💒"); travel.add("🏛️"); travel.add("⛪"); travel.add("🕌");
        travel.add("🕍"); travel.add("🛕"); travel.add("🕋");
        EMOJI_MAP.put(Category.TRAVEL, travel);
        
        // Activities
        List<String> activities = new ArrayList<>();
        activities.add("⚽"); activities.add("🏀"); activities.add("🏈"); activities.add("⚾");
        activities.add("🥎"); activities.add("🎾"); activities.add("🏐"); activities.add("🏉");
        activities.add("🥏"); activities.add("🎱"); activities.add("🪀"); activities.add("🏓");
        activities.add("🏸"); activities.add("🏒"); activities.add("🏑"); activities.add("🥍");
        activities.add("🏏"); activities.add("🥅"); activities.add("⛳"); activities.add("🪁");
        activities.add("🏹"); activities.add("🎣"); activities.add("🤿"); activities.add("🥊");
        activities.add("🥋"); activities.add("🎽"); activities.add("🛹"); activities.add("🛷");
        activities.add("⛸️"); activities.add("🥌"); activities.add("🎿"); activities.add("⛷️");
        activities.add("🏂"); activities.add("🪂"); activities.add("🏋️"); activities.add("🤼");
        activities.add("🤸"); activities.add("🤺"); activities.add("⛹️"); activities.add("🤾");
        activities.add("🏌️"); activities.add("🏇"); activities.add("🧘"); activities.add("🏄");
        activities.add("🏊"); activities.add("🤽"); activities.add("🚣"); activities.add("🧗");
        activities.add("🚵"); activities.add("🚴"); activities.add("🏆"); activities.add("🥇");
        activities.add("🥈"); activities.add("🥉"); activities.add("🏅"); activities.add("🎖️");
        activities.add("🏵️"); activities.add("🎗️"); activities.add("🎫"); activities.add("🎟️");
        activities.add("🎪"); activities.add("🤹"); activities.add("🎭"); activities.add("🩰");
        activities.add("🎨"); activities.add("🎬"); activities.add("🎤"); activities.add("🎧");
        activities.add("🎼"); activities.add("🎹"); activities.add("🥁"); activities.add("🎷");
        activities.add("🎺"); activities.add("🎸"); activities.add("🪕"); activities.add("🎻");
        activities.add("🎲"); activities.add("♟️"); activities.add("🎯"); activities.add("🎳");
        activities.add("🎮"); activities.add("🎰"); activities.add("🧩");
        EMOJI_MAP.put(Category.ACTIVITIES, activities);
        
        // Objects
        List<String> objects = new ArrayList<>();
        objects.add("⌚"); objects.add("📱"); objects.add("📲"); objects.add("💻");
        objects.add("⌨️"); objects.add("🖥️"); objects.add("🖨️"); objects.add("🖱️");
        objects.add("🖲️"); objects.add("🕹️"); objects.add("🗜️"); objects.add("💽");
        objects.add("💾"); objects.add("💿"); objects.add("📀"); objects.add("📼");
        objects.add("📷"); objects.add("📸"); objects.add("📹"); objects.add("🎥");
        objects.add("📽️"); objects.add("🎞️"); objects.add("📞"); objects.add("☎️");
        objects.add("📟"); objects.add("📠"); objects.add("📺"); objects.add("📻");
        objects.add("🎙️"); objects.add("🎚️"); objects.add("🎛️"); objects.add("🧭");
        objects.add("⏱️"); objects.add("⏲️"); objects.add("⏰"); objects.add("🕰️");
        objects.add("⌛"); objects.add("⏳"); objects.add("📡"); objects.add("🔋");
        objects.add("🔌"); objects.add("💡"); objects.add("🔦"); objects.add("🕯️");
        objects.add("🪔"); objects.add("🧯"); objects.add("🛢️"); objects.add("💸");
        objects.add("💵"); objects.add("💴"); objects.add("💶"); objects.add("💷");
        objects.add("💰"); objects.add("💳"); objects.add("💎"); objects.add("⚖️");
        objects.add("🧰"); objects.add("🔧"); objects.add("🔨"); objects.add("⚒️");
        objects.add("🛠️"); objects.add("⛏️"); objects.add("🔩"); objects.add("⚙️");
        objects.add("🧱"); objects.add("⛓️"); objects.add("🧲"); objects.add("🔫");
        objects.add("💣"); objects.add("🧨"); objects.add("🪓"); objects.add("🔪");
        objects.add("🗡️"); objects.add("⚔️"); objects.add("🛡️"); objects.add("🚬");
        objects.add("⚰️"); objects.add("⚱️"); objects.add("🏺"); objects.add("🔮");
        objects.add("📿"); objects.add("🧿"); objects.add("💈"); objects.add("⚗️");
        objects.add("🔭"); objects.add("🔬"); objects.add("🕳️"); objects.add("🩹");
        objects.add("🩺"); objects.add("💊"); objects.add("💉"); objects.add("🩸");
        objects.add("🧬"); objects.add("🦠"); objects.add("🧫"); objects.add("🧪");
        objects.add("🌡️"); objects.add("🧹"); objects.add("🧺"); objects.add("🧻");
        objects.add("🚽"); objects.add("🚰"); objects.add("🚿"); objects.add("🛁");
        objects.add("🛀"); objects.add("🧼"); objects.add("🪒"); objects.add("🧽");
        objects.add("🧴"); objects.add("🛎️"); objects.add("🔑"); objects.add("🗝️");
        objects.add("🚪"); objects.add("🪑"); objects.add("🛋️"); objects.add("🛏️");
        objects.add("🧸"); objects.add("🖼️"); objects.add("🛍️"); objects.add("🎁");
        objects.add("🎈"); objects.add("🎏"); objects.add("🎀"); objects.add("🎊");
        objects.add("🎉"); objects.add("🎎"); objects.add("🏮"); objects.add("🎐");
        objects.add("🧧"); objects.add("✉️"); objects.add("📩"); objects.add("📨");
        objects.add("📧"); objects.add("💌"); objects.add("📥"); objects.add("📤");
        objects.add("📦"); objects.add("🏷️"); objects.add("📪"); objects.add("📫");
        objects.add("📬"); objects.add("📭"); objects.add("📮"); objects.add("📯");
        objects.add("📜"); objects.add("📃"); objects.add("📄"); objects.add("📑");
        objects.add("🧾"); objects.add("📊"); objects.add("📈"); objects.add("📉");
        objects.add("🗒️"); objects.add("🗓️"); objects.add("📆"); objects.add("📅");
        objects.add("🗑️"); objects.add("📇"); objects.add("🗃️"); objects.add("🗳️");
        objects.add("🗄️"); objects.add("📋"); objects.add("📁"); objects.add("📂");
        objects.add("🗂️"); objects.add("🗞️"); objects.add("📰"); objects.add("📓");
        objects.add("📔"); objects.add("📒"); objects.add("📕"); objects.add("📗");
        objects.add("📘"); objects.add("📙"); objects.add("📚"); objects.add("📖");
        objects.add("🔖"); objects.add("🧷"); objects.add("🔗"); objects.add("📎");
        objects.add("🖇️"); objects.add("📐"); objects.add("📏"); objects.add("🧮");
        objects.add("📌"); objects.add("📍"); objects.add("✂️"); objects.add("🖊️");
        objects.add("🖋️"); objects.add("✒️"); objects.add("🖌️"); objects.add("🖍️");
        objects.add("📝"); objects.add("✏️"); objects.add("🔍"); objects.add("🔎");
        objects.add("🔏"); objects.add("🔐"); objects.add("🔒"); objects.add("🔓");
        EMOJI_MAP.put(Category.OBJECTS, objects);
        
        // Symbols
        List<String> symbols = new ArrayList<>();
        symbols.add("❤️"); symbols.add("🧡"); symbols.add("💛"); symbols.add("💚");
        symbols.add("💙"); symbols.add("💜"); symbols.add("🖤"); symbols.add("🤍");
        symbols.add("🤎"); symbols.add("💔"); symbols.add("❣️"); symbols.add("💕");
        symbols.add("💞"); symbols.add("💓"); symbols.add("💗"); symbols.add("💖");
        symbols.add("💘"); symbols.add("💝"); symbols.add("💟"); symbols.add("☮️");
        symbols.add("✝️"); symbols.add("☪️"); symbols.add("🕉️"); symbols.add("☸️");
        symbols.add("✡️"); symbols.add("🔯"); symbols.add("🕎"); symbols.add("☯️");
        symbols.add("☦️"); symbols.add("🛐"); symbols.add("⛎"); symbols.add("♈");
        symbols.add("♉"); symbols.add("♊"); symbols.add("♋"); symbols.add("♌");
        symbols.add("♍"); symbols.add("♎"); symbols.add("♏"); symbols.add("♐");
        symbols.add("♑"); symbols.add("♒"); symbols.add("♓"); symbols.add("🆔");
        symbols.add("⚛️"); symbols.add("🉑"); symbols.add("☢️"); symbols.add("☣️");
        symbols.add("📴"); symbols.add("📳"); symbols.add("🈶"); symbols.add("🈚");
        symbols.add("🈸"); symbols.add("🈺"); symbols.add("🈷️"); symbols.add("✴️");
        symbols.add("🆚"); symbols.add("💮"); symbols.add("🉐"); symbols.add("㊙️");
        symbols.add("㊗️"); symbols.add("🈴"); symbols.add("🈵"); symbols.add("🈹");
        symbols.add("🈲"); symbols.add("🅰️"); symbols.add("🅱️"); symbols.add("🆎");
        symbols.add("🆑"); symbols.add("🅾️"); symbols.add("🆘"); symbols.add("❌");
        symbols.add("⭕"); symbols.add("🛑"); symbols.add("⛔"); symbols.add("📛");
        symbols.add("🚫"); symbols.add("💯"); symbols.add("💢"); symbols.add("♨️");
        symbols.add("🚷"); symbols.add("🚯"); symbols.add("🚳"); symbols.add("🚱");
        symbols.add("🔞"); symbols.add("📵"); symbols.add("🚭"); symbols.add("❗");
        symbols.add("❕"); symbols.add("❓"); symbols.add("❔"); symbols.add("‼️");
        symbols.add("⁉️"); symbols.add("🔅"); symbols.add("🔆"); symbols.add("〽️");
        symbols.add("⚠️"); symbols.add("🚸"); symbols.add("🔱"); symbols.add("⚜️");
        symbols.add("🔰"); symbols.add("♻️"); symbols.add("✅"); symbols.add("🈯");
        symbols.add("💹"); symbols.add("❇️"); symbols.add("✳️"); symbols.add("❎");
        symbols.add("🌐"); symbols.add("💠"); symbols.add("Ⓜ️"); symbols.add("🌀");
        symbols.add("💤"); symbols.add("🏧"); symbols.add("🚾"); symbols.add("♿");
        symbols.add("🅿️"); symbols.add("🈳"); symbols.add("🈂️"); symbols.add("🛂");
        symbols.add("🛃"); symbols.add("🛄"); symbols.add("🛅"); symbols.add("🚹");
        symbols.add("🚺"); symbols.add("🚼"); symbols.add("⚧️"); symbols.add("🚻");
        symbols.add("🚮"); symbols.add("🎦"); symbols.add("📶"); symbols.add("🈁");
        symbols.add("🔣"); symbols.add("ℹ️"); symbols.add("🔤"); symbols.add("🔡");
        symbols.add("🔠"); symbols.add("🆖"); symbols.add("🆗"); symbols.add("🆙");
        symbols.add("🆒"); symbols.add("🆕"); symbols.add("🆓"); symbols.add("0️⃣");
        symbols.add("1️⃣"); symbols.add("2️⃣"); symbols.add("3️⃣"); symbols.add("4️⃣");
        symbols.add("5️⃣"); symbols.add("6️⃣"); symbols.add("7️⃣"); symbols.add("8️⃣");
        symbols.add("9️⃣"); symbols.add("🔟"); symbols.add("🔢"); symbols.add("#️⃣");
        symbols.add("*️⃣"); symbols.add("⏏️"); symbols.add("▶️"); symbols.add("⏸️");
        symbols.add("⏯️"); symbols.add("⏹️"); symbols.add("⏺️"); symbols.add("⏭️");
        symbols.add("⏮️"); symbols.add("⏩"); symbols.add("⏪"); symbols.add("⏫");
        symbols.add("⏬"); symbols.add("◀️"); symbols.add("🔼"); symbols.add("🔽");
        symbols.add("➡️"); symbols.add("⬅️"); symbols.add("⬆️"); symbols.add("⬇️");
        symbols.add("↗️"); symbols.add("↘️"); symbols.add("↙️"); symbols.add("↖️");
        symbols.add("↕️"); symbols.add("↔️"); symbols.add("↪️"); symbols.add("↩️");
        symbols.add("⤴️"); symbols.add("⤵️"); symbols.add("🔀"); symbols.add("🔁");
        symbols.add("🔂"); symbols.add("🔄"); symbols.add("🔃"); symbols.add("🎵");
        symbols.add("🎶"); symbols.add("➕"); symbols.add("➖"); symbols.add("➗");
        symbols.add("✖️"); symbols.add("♾️"); symbols.add("💲"); symbols.add("💱");
        symbols.add("™️"); symbols.add("©️"); symbols.add("®️"); symbols.add("〰️");
        symbols.add("➰"); symbols.add("➿"); symbols.add("🔚"); symbols.add("🔙");
        symbols.add("🔛"); symbols.add("🔝"); symbols.add("🔜"); symbols.add("✔️");
        symbols.add("☑️"); symbols.add("🔘"); symbols.add("🔴"); symbols.add("🟠");
        symbols.add("🟡"); symbols.add("🟢"); symbols.add("🔵"); symbols.add("🟣");
        symbols.add("⚫"); symbols.add("⚪"); symbols.add("🟤"); symbols.add("🔺");
        symbols.add("🔻"); symbols.add("🔸"); symbols.add("🔹"); symbols.add("🔶");
        symbols.add("🔷"); symbols.add("🔳"); symbols.add("🔲"); symbols.add("▪️");
        symbols.add("▫️"); symbols.add("◾"); symbols.add("◽"); symbols.add("◼️");
        symbols.add("◻️"); symbols.add("🟥"); symbols.add("🟧"); symbols.add("🟨");
        symbols.add("🟩"); symbols.add("🟦"); symbols.add("🟪"); symbols.add("⬛");
        symbols.add("⬜"); symbols.add("🟫"); symbols.add("🔈"); symbols.add("🔇");
        symbols.add("🔉"); symbols.add("🔊"); symbols.add("🔔"); symbols.add("🔕");
        symbols.add("📣"); symbols.add("📢"); symbols.add("👁️‍🗨️"); symbols.add("💬");
        symbols.add("💭"); symbols.add("🗯️"); symbols.add("♠️"); symbols.add("♣️");
        symbols.add("♥️"); symbols.add("♦️"); symbols.add("🃏"); symbols.add("🎴");
        symbols.add("🀄"); symbols.add("🕐"); symbols.add("🕑"); symbols.add("🕒");
        symbols.add("🕓"); symbols.add("🕔"); symbols.add("🕕"); symbols.add("🕖");
        symbols.add("🕗"); symbols.add("🕘"); symbols.add("🕙"); symbols.add("🕚");
        symbols.add("🕛"); symbols.add("🕜"); symbols.add("🕝"); symbols.add("🕞");
        symbols.add("🕟"); symbols.add("🕠"); symbols.add("🕡"); symbols.add("🕢");
        symbols.add("🕣"); symbols.add("🕤"); symbols.add("🕥"); symbols.add("🕦");
        symbols.add("🕧");
        EMOJI_MAP.put(Category.SYMBOLS, symbols);
        
        // Flags (Sample - TR + Popular)
        List<String> flags = new ArrayList<>();
        flags.add("🇹🇷"); flags.add("🇺🇸"); flags.add("🇬🇧"); flags.add("🇩🇪");
        flags.add("🇫🇷"); flags.add("🇮🇹"); flags.add("🇪🇸"); flags.add("🇷🇺");
        flags.add("🇨🇳"); flags.add("🇯🇵"); flags.add("🇰🇷"); flags.add("🇮🇳");
        flags.add("🇧🇷"); flags.add("🇲🇽"); flags.add("🇦🇷"); flags.add("🇨🇦");
        flags.add("🇦🇺"); flags.add("🇿🇦"); flags.add("🇸🇦"); flags.add("🇦🇪");
        flags.add("🇪🇬"); flags.add("🇳🇱"); flags.add("🇧🇪"); flags.add("🇨🇭");
        flags.add("🇦🇹"); flags.add("🇬🇷"); flags.add("🇵🇱"); flags.add("🇸🇪");
        flags.add("🇳🇴"); flags.add("🇩🇰"); flags.add("🇫🇮"); flags.add("🇵🇹");
        flags.add("🇮🇪"); flags.add("🇮🇱"); flags.add("🇮🇷"); flags.add("🇮🇶");
        flags.add("🇵🇰"); flags.add("🇦🇫"); flags.add("🇧🇩"); flags.add("🇱🇰");
        flags.add("🇲🇲"); flags.add("🇹🇭"); flags.add("🇻🇳"); flags.add("🇮🇩");
        flags.add("🇵🇭"); flags.add("🇲🇾"); flags.add("🇸🇬"); flags.add("🇳🇿");
        EMOJI_MAP.put(Category.FLAGS, flags);
    }
    
    public static List<String> getEmojis(Category category) {
        return EMOJI_MAP.get(category);
    }
    
    public static Category[] getAllCategories() {
        return Category.values();
    }
    
    public static String[] getAllCategoryIcons() {
        Category[] categories = getAllCategories();
        String[] icons = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            icons[i] = categories[i].getIcon();
        }
        return icons;
    }
    
    public static Category getCategoryByIcon(String icon) {
        for (Category cat : getAllCategories()) {
            if (cat.getIcon().equals(icon)) {
                return cat;
            }
        }
        return Category.SMILEYS;
    }
}

