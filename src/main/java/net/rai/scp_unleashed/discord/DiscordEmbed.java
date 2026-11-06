package net.rai.scp_unleashed.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiscordEmbed {
    private String Title = "";
    private String Description = "";
    private String Thumbnail = "";
    private String AuthorName = "";
    private String AuthorIcon = "";
    private int Colour = -1;
    private final List<Field> Fields = new ArrayList<>();

    public record Field(String Name, String Value, boolean Inline) {
    }

    public DiscordEmbed title(String Text) {
        Title = DiscordText.clip(DiscordText.escape(Text), DiscordText.TitleLimit);
        return this;
    }

    public DiscordEmbed description(String Text) {
        Description = DiscordText.clip(DiscordText.escape(Text), DiscordText.DescriptionLimit);
        return this;
    }

    public DiscordEmbed quote(String Text) {
        String Escaped = DiscordText.escape(Text);
        Description = DiscordText.clip(Escaped.isEmpty() ? "" : ">>> " + Escaped, DiscordText.DescriptionLimit);
        return this;
    }

    public DiscordEmbed thumbnail(String Url) {
        Thumbnail = DiscordUrls.isImage(Url) ? Url : "";
        return this;
    }

    public DiscordEmbed author(String Name, String Icon) {
        AuthorName = DiscordText.clip(DiscordText.flat(Name), DiscordText.AuthorLimit);
        AuthorIcon = DiscordUrls.isImage(Icon) ? Icon : "";
        return this;
    }

    public DiscordEmbed colour(int Value) {
        Colour = Value & 0xFFFFFF;
        return this;
    }

    public DiscordEmbed field(String Name, String Value, boolean Inline) {
        return rawField(Name, DiscordText.escape(Value), Inline);
    }

    public DiscordEmbed rawField(String Name, String Value, boolean Inline) {
        if (Fields.size() >= DiscordText.FieldCount || Value == null || Value.isBlank()) {
            return this;
        }
        String CleanName = DiscordText.clip(DiscordText.escape(Name), DiscordText.FieldNameLimit);
        Fields.add(new Field(CleanName.isBlank() ? "\u200B" : CleanName, DiscordText.clip(Value, DiscordText.FieldValueLimit), Inline));
        return this;
    }

    public DiscordEmbed position(BlockPos Pos, ResourceKey<Level> Dimension) {
        return position(Pos, Dimension.location().toString());
    }

    public DiscordEmbed position(BlockPos Pos, String Dimension) {
        rawField(DiscordText.translate("discord.scp_unleashed.field.position"), code(Pos), true);
        String Raw = LeadPlanner.dimensionName(Dimension).getString();
        String Named = Raw.isEmpty() || Raw.equals(Dimension) ? Raw : Character.toUpperCase(Raw.charAt(0)) + Raw.substring(1);
        String Shown = Named.isEmpty() || Named.equals(Dimension) ? "`" + Dimension.replace("`", "") + "`" : DiscordText.escape(Named);
        return rawField(DiscordText.translate("discord.scp_unleashed.field.dimension"), Shown, true);
    }

    public static String code(BlockPos Pos) {
        return String.format(Locale.ROOT, "`%d %d %d`", Pos.getX(), Pos.getY(), Pos.getZ());
    }

    public String getTitle() {
        return Title;
    }

    public String getDescription() {
        return Description;
    }

    public String getThumbnail() {
        return Thumbnail;
    }

    public List<Field> getFields() {
        return List.copyOf(Fields);
    }

    public JsonObject toJson(int FallbackColour, String Footer, Instant Stamp) {
        String CleanFooter = DiscordText.clip(DiscordText.flat(Footer), DiscordText.FooterLimit);
        int Budget = DiscordText.TotalLimit - Title.length() - CleanFooter.length() - AuthorName.length();
        List<Field> Kept = new ArrayList<>();
        int Used = 0;
        for (Field Entry : Fields) {
            int Cost = Entry.Name().length() + Entry.Value().length();
            if (Used + Cost > Budget) {
                break;
            }
            Kept.add(Entry);
            Used += Cost;
        }
        String Body = Description;
        int Room = Math.max(0, Budget - Used);
        if (Body.length() > Room) {
            Body = Room <= DiscordText.Ellipsis.length() ? "" : DiscordText.clip(Body, Room);
        }
        JsonObject Embed = new JsonObject();
        if (!Title.isEmpty()) {
            Embed.addProperty("title", Title);
        }
        if (!Body.isEmpty()) {
            Embed.addProperty("description", Body);
        }
        Embed.addProperty("color", Colour >= 0 ? Colour : FallbackColour & 0xFFFFFF);
        Embed.addProperty("timestamp", DateTimeFormatter.ISO_INSTANT.format(Stamp));
        if (!AuthorName.isEmpty()) {
            JsonObject Author = new JsonObject();
            Author.addProperty("name", AuthorName);
            if (!AuthorIcon.isEmpty()) {
                Author.addProperty("icon_url", AuthorIcon);
            }
            Embed.add("author", Author);
        }
        if (!Thumbnail.isEmpty()) {
            JsonObject Picture = new JsonObject();
            Picture.addProperty("url", Thumbnail);
            Embed.add("thumbnail", Picture);
        }
        if (!Kept.isEmpty()) {
            JsonArray Rows = new JsonArray();
            for (Field Entry : Kept) {
                JsonObject Shaped = new JsonObject();
                Shaped.addProperty("name", Entry.Name());
                Shaped.addProperty("value", Entry.Value());
                Shaped.addProperty("inline", Entry.Inline());
                Rows.add(Shaped);
            }
            Embed.add("fields", Rows);
        }
        if (!CleanFooter.isEmpty()) {
            JsonObject Foot = new JsonObject();
            Foot.addProperty("text", CleanFooter);
            Embed.add("footer", Foot);
        }
        return Embed;
    }

    public static JsonObject payload(DiscordSettings Settings, DiscordCategory Category, DiscordEmbed Embed, Instant Stamp) {
        JsonObject Root = new JsonObject();
        if (!Settings.Username().isEmpty()) {
            Root.addProperty("username", DiscordText.clip(Settings.Username(), DiscordText.UsernameLimit));
        }
        if (DiscordUrls.isImage(Settings.AvatarUrl())) {
            Root.addProperty("avatar_url", Settings.AvatarUrl());
        }
        JsonObject Mentions = new JsonObject();
        Mentions.add("parse", new JsonArray());
        Root.add("allowed_mentions", Mentions);
        String SectionName = DiscordText.translate(Category.langKey());
        String Footer = Settings.ServerName().isEmpty() ? SectionName : Settings.ServerName() + " \u2022 " + SectionName;
        JsonArray Embeds = new JsonArray();
        Embeds.add(Embed.toJson(Settings.channel(Category).Colour(), Footer, Stamp));
        Root.add("embeds", Embeds);
        return Root;
    }
}
