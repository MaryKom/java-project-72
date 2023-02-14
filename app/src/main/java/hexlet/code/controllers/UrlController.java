package hexlet.code.controllers;
import hexlet.code.model.Url;
import hexlet.code.model.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class UrlController {
    public static Handler addUrl = ctx -> {
        String parsedUrl = parseUrl(ctx.formParam("url"));
        if (parsedUrl == null) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
        } else {
            Url url = new QUrl()
                    .name.equalTo(parsedUrl)
                    .findOne();
            if (url == null) {
                url = new Url(parsedUrl);
                url.save();
                ctx.sessionAttribute("flash", "Страница успешно добавлена");
                ctx.sessionAttribute("flash-type", "success");
            } else {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.sessionAttribute("flash-type", "info");
            }
            ctx.redirect("/urls");
        }
    };

    public static String parseUrl(String inputUrl) {
        try {
            URL url = new URL(inputUrl);
            String protocol = url.getProtocol();
            String authority = url.getAuthority();
            return protocol + "://" + authority;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static Handler listUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int rowsPerPage = 10;
        int offset = (page - 1) * rowsPerPage;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        ctx.attribute("urls", urls);
        ctx.attribute("page", page);
        ctx.render("urls.html");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        ctx.attribute("url", url);
        ctx.render("articles/show.html");
    };

}
