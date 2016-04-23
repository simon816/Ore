var PROJECTS_PER_CLICK = 50;
var CATEGORY_STRING = null;
var SORT_STRING = null;

var currentlyLoaded = 0;

$(function() {

    // Initialize sorting selection
    $('.select-sort').on('change', function() {
        var url = '/?sort=' + $(this).find('option:selected').val();
        if (CATEGORY_STRING) url += '&categories=' + CATEGORY_STRING;
        window.location = url;
    });

    // Initialize more button
    $('.btn-more').click(function() {
        var ajaxUrl = '/api/projects?limit=' + PROJECTS_PER_CLICK + '&offset=' + currentlyLoaded;
        if (CATEGORY_STRING) ajaxUrl += '&categories=' + CATEGORY_STRING;
        if (SORT_STRING) ajaxUrl += '&sort=' + SORT_STRING;

        // Request more projects
        $('.btn-more').html('<i class="fa fa-spinner fa-spin"></i>');
        $.ajax({
            url: ajaxUrl,
            dataType: 'json',
            success: function(data) {
                for (var i in data) {
                    if (!data.hasOwnProperty(i)) {
                        continue;
                    }
                    var project = data[i];
                    var category = project.category;

                    // Add received project to table
                    // TODO: Use template in HTML
                    $('.project-table').find('tbody').append(
                          '<tr>'
                          + '<td><i title="' + category.title + '" class="fa ' + category.icon + '"></i></td>'
                          + '<td class="name">'
                          +   '<strong><a href="' + project.href + '">' + project.name + '</a></strong>'
                          +   '<i title="' + project.description + '" class="minor"> ' + project.description + '</i>'
                          + '</td>'
                          + '<td class="author"><a href="#">' + project.owner + '</a></td>'
                          + '<td class="minor">' + project.views + '</td>'
                          + '<td class="minor">' + project.downloads + '</td>'
                          + '<td class="minor">' + project.stars + '</td>'
                        + '</tr>'
                    );
                }
                currentlyLoaded += data.length;
                $('.btn-more').html('<strong>More</strong>');
            }
        });
    });
});
