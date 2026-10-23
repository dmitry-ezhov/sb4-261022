package com.example.library.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Copy as the database records it: a row of the copy table.
 *
 * <p>An entity, because the persistence framework needs one - a class with
 * accessors, whose field names name the columns - and not a payload: it does
 * not leave the persistence layer, and the API speaks the {@link Copy}
 * record instead. Its accessors exist for the framework and for the
 * queries built on the field names, not for callers.
 */
@Entity
@Table(name = "copy")
public class CopyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Title this Copy is an item of, by the Title's key. */
    @Column(name = "title_id", nullable = false)
    private Long titleId;

    /** The library's barcode, unique across every Copy. */
    @Column(nullable = false, unique = true)
    private String barcode;

    /** Whether the Copy is currently on loan. */
    @Column(name = "on_loan", nullable = false)
    private boolean onLoan;

    /** For the persistence framework alone; the application never calls it. */
    CopyEntity() {}

    /** A Copy as the library records it: an item of exactly one Title. */
    CopyEntity(Long titleId, String barcode, boolean onLoan) {
        this.titleId = titleId;
        this.barcode = barcode;
        this.onLoan = onLoan;
    }

    public Long getId() {
        return id;
    }

    public Long getTitleId() {
        return titleId;
    }

    public String getBarcode() {
        return barcode;
    }

    public boolean isOnLoan() {
        return onLoan;
    }
}
