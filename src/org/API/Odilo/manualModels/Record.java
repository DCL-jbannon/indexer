package org.API.Odilo.manualModels;

import javax.xml.xpath.XPathConstants;

/**
 * Created by jbannon on 8/28/2014.
 */
public class Record {
    private String isbn;
    private String description;
    private String author;
    private String subject;
    private String title;
    private String subtitle;
    private String language;
    private String publisher;
    private String edition;
    private String targetAudience;
    private String publishDate;
    private String externalId;

    public Record(String isbn, String description, String author, String subject, String title, String subtitle, String language, String publisher, String edition, String targetAudience, String publishDate) {
        this.isbn = isbn;
        this.description = description;
        this.author = author;
        this.subject = subject;
        this.title = title;
        this.subtitle = subtitle;
        this.language = language;
        this.publisher = publisher;
        this.edition = edition;
        this.targetAudience = targetAudience;
        this.publishDate = publishDate;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getLanguage() {
        return language;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getEdition() {
        return edition;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
