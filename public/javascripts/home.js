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
                    var projectRow = $('#row-project').clone().attr('id', '');
                    var nameCol = projectRow.find('.name');
                    projectRow.find('.category').find('i').attr('title', category.title).addClass(category.icon);
                    nameCol.find('strong').find('a').attr('href', project.href).text(project.name);
                    nameCol.find('i').attr('title', project.description).text(project.description);
                    projectRow.find('.author').find('a').attr('href', '/' + project.owner).text(project.owner);
                    projectRow.find('.views').text(project.views);
                    projectRow.find('.downloads').text(project.downloads);
                    projectRow.find('.stars').text(project.stars);
                    $('.project-table').find('tbody').append(projectRow);
                }
                currentlyLoaded += data.length;
                $('.btn-more').html('<strong>More</strong>');
            }
        });
    });
});
