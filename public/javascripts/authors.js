var CURRENT_PAGE = 0;

$(function() {

    $('.table-authors').find('thead').find('td:not(:first-child)').click(function() {
        var sort = $(this).text().toLowerCase().trim();
        var direction = '';
        if ($(this).hasClass('author-sort')) {
            // Change direction
            direction = $(this).find('i').hasClass('o-chevron-up') ? '-' : '';
        }
        var url = '/authors?sort=' + direction + sort;
        if (CURRENT_PAGE > 1) url += '&page=' + CURRENT_PAGE;
        go(url);
    });

});
