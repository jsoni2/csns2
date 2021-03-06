<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="csns" uri="http://cs.calstatela.edu/csns" %>

<script>
$(function(){
    $(".add").autocomplete({
        source: "<c:url value='/autocomplete/course' />",
        select: function(event, ui) {
            if( ui.item )
            {
                $("<span>").attr({
                    id: $(this).attr("id") + "-" + ui.item.id
                }).append(
                    $("<input>").attr({
                        type: "hidden",
                        name: $(this).attr("id"),
                        value: ui.item.id
                    })
                ).append(
                    $("<a>").attr({
                        href: "javascript:delete" + $(this).attr("id") + "(" + ui.item.id + ")"
                    }).text(ui.item.value)
                ).append(", ").insertBefore($(this));
                event.preventDefault();
                $(this).val("");
            }
        }
    });
    $("#coordinator").autocomplete({
        source: "<c:url value='/autocomplete/user' />",
        select: function(event, ui) {
            if( ui.item )
                $("input[name='coordinator']").val(ui.item.id);
        }
    });
    $("div.help").dialog({
        autoOpen: false,
        modal: true
    });
});
function help( name )
{
    $("#help-"+name).dialog("open");
}
function deleteprerequisites( prereqId )
{
    var msg = "Are you sure you want to remove this prerequisite?";
    if( confirm(msg) )
      $("#prerequisites-"+prereqId).remove();
}
</script>

<ul id="title">
<c:choose>
  <c:when test="${not empty dept}">
    <li><a class="bc" href="<c:url value='/department/${dept}/courses' />">Courses</a></li>
  </c:when>
  <c:otherwise>
    <li><a class="bc" href="search">Courses</a></li>
  </c:otherwise>
</c:choose>
<li><a class="bc" href="view?id=${course.id}">${course.code}</a></li>
<li>Edit</li>
</ul>

<form:form modelAttribute="course" enctype="multipart/form-data">
<table class="general autowidth">
  <tr>
    <th><csns:help name="code">Code</csns:help> *</th>
    <td>
      <form:input path="code" cssClass="forminput" cssStyle="width: 100px;" />
      <div class="error"><form:errors path="code" /></div>
    </td>
  </tr>
  <tr>
    <th>Name *</th>
    <td>
      <form:input path="name" cssClass="forminput" cssStyle="width: 600px;" />
      <div class="error"><form:errors path="name" /></div>
    </td>
  </tr>
  <tr>
    <th>Prerequisites</th>
    <td>
      <c:forEach items="${course.prerequisites}" var="prerequisite" varStatus="status">
        <span id="prerequisites-${prerequisite.id}">
          <input type="hidden" name="prerequisites" value="${prerequisite.id}" />
          <a href="javascript:deleteprerequisites(${prerequisite.id})">${prerequisite.code}</a>,
        </span>
      </c:forEach>
      <input id="prerequisites" type="text" class="forminput add" name="a" style="width: 100px;" />
    </td>
  </tr>
  <tr>
    <th>Coordinator</th>
    <td>
      <input id="coordinator" name="cname" class="forminput" value="${course.coordinator.name}" style="width: 600px;" />
      <input name="coordinator" type="hidden" value="${course.coordinator.id}" />
    </td>
  </tr>
  <tr>
    <th>Description</th>
    <td>
      <input name="file" type="file" class="forminput" style="width: 600px;" />
    </td>
  </tr>
  <tr>
    <th>Obsolete</th>
    <td><form:checkbox path="obsolete" cssStyle="width: auto;" /></td>
  </tr>
  <tr>
    <th></th>
    <td>
      <input type="submit" class="subbutton" value="Save" />
    </td>
  </tr>
</table>
</form:form>

<div id="help-code" class="help">A course code must consist of uppercase letters
followed by a number, and optionally, followed by another uppercase letter, e.g.
<span class="tt">CS101</span> or <span class="tt">CS496A</span>.</div>
