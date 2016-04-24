var PROJECTS_PER_CLICK = 50;
var CATEGORY_STRING = null;
var SORT_STRING = null;
var QUERY_STRING = null;

var currentlyLoaded = 0;

$(function() {

    // Setup category table
    $('.category-table').find('tr').click(function() {
        var categoryString = '';
        var id = $(this).data('id');
        if ($(this).hasClass('selected')) {
            // Category is already selected
            var self = $(this);
            var selected = $('.category-table').find('.selected');
            selected.each(function(i) {
                if ($(this).is(self)) return; // Skip the clicked category
                categoryString += $(this).data('id');
                if (i < selected.length - 1) categoryString += ',';
            });
        } else if (CATEGORY_STRING) {
            categoryString += CATEGORY_STRING + ',' + $(this).data('id');
        } else {
            categoryString += id;
        }

        // Build URL
        var url = '/?';
        if (categoryString.length > 0) {
            url += 'categories=' + categoryString;
            if (SORT_STRING) url += '&sort=' + SORT_STRING;
        } else if (SORT_STRING) {
            url += 'sort=' + SORT_STRING;
        }

        // Fly you fools!
        window.location = url;
    });

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
        if (QUERY_STRING) ajaxUrl += '&q=' + QUERY_STRING;

        // Request more projects
        $('.btn-more').html('<i class="fa fa-spinner fa-spin"></i>');
        $.ajax({
            url: ajaxUrl,
            dataType: 'json',
            success: function(projects) {
                for (var i in projects) {
                    if (!projects.hasOwnProperty(i)) continue;
                    var project = projects[i];
                    var category = project.category;

                    // Add received project to table
                    var projectRow = $('#row-project').clone().removeAttr('id');
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
                currentlyLoaded += projects.length;
                $('.btn-more').html('<strong>More</strong>');
            }
        });
    });
});
