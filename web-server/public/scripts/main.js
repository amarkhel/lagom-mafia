$(document).ready(function(){
    $('a.sectionLink').click(function(event) {
        var id = $(this).attr('id');
        $('div.section').addClass('hide');
        $('div.' + id).removeClass('hide');
        $('a.sectionLink').parent().removeClass('active');
        $(this).parent().addClass('active');

        return false;
    });
});