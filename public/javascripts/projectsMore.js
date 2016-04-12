var PROJECTS_PER_CLICK = 50;
var CATEGORY_STRING = null;

var currentlyLoaded = 0;

$(function() {
    $('.alert').fadeIn('slow');

    $('.btn-more').click(function() {
        var ajaxUrl = '/api/projects?limit=' + PROJECTS_PER_CLICK + '&offset=' + currentlyLoaded;
        if (CATEGORY_STRING) {
            ajaxUrl += '&categories=' + CATEGORY_STRING;
        }
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
                    var url = '/' + project.owner + '/' + project.name;
                    $('.project-table').find('tbody').append(
                          '<tr>'
                          + '<td><i title="' + category.title + '" class="fa ' + category.icon + '"></i></td>'
                          + '<td class="name">'
                          +   '<strong><a href="' + url + '">' + project.name + '</a></strong>'
                          +   '<i class="minor"> ' + project.description + '</i>'
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
