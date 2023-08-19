package me.antonionoack.ircontrol

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import me.antonionoack.ircontrol.ir.CommandLogic
import me.antonionoack.ircontrol.ir.CommandLogic.loadCurrentProject
import me.antonionoack.ircontrol.ir.CommandLogic.save


object Projects {

    val projectNames = ArrayList<String>()
    var projectName = ""

    private fun getNameError(name: String): String? {
        if (name.isEmpty()) return "Name must not be blank!"
        for (it in name) {
            if (it !in 'A'..'Z' && it !in 'a'..'z' && it !in
                '0'..'9' && it !in " .,-_/"
            ) return "Forbidden character: '$it'"
        }
        if (name.startsWith("/") || name.endsWith("/"))
            return "Project must not start/end with slash"
        if (name in projectNames) return "Project already exists"
        return null
    }

    fun MainActivity.deleteProject(project: String) {
        projectNames.remove(project)
        if (project == projectName) {
            projectName = ""
            CommandLogic.sequence.clear()
        }
        save()
    }

    class Folder(val name: String, val group: ViewGroup)

    @SuppressLint("SetTextI18n", "InflateParams")
    fun MainActivity.addAllProjectButtons(projectList: ViewGroup) {
        projectList.removeAllViews()
        val folderStack = ArrayList<Folder>()

        val padding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25f, resources.displayMetrics)
                .toInt()
        for (project in projectNames) {

            // add folders
            var parts = project.split('/').map { it.trim() }
            val shortName = parts.last()
            parts = parts.subList(0, parts.lastIndex)
            // move from folder to parts
            var i = 0
            while (i < kotlin.math.min(folderStack.size, parts.size)) {
                if (parts[i] != folderStack[i].name) break
                i++
            }
            for (j in folderStack.lastIndex downTo i) folderStack.removeAt(j)
            while (i < parts.size) {
                // add new part of path
                val folderButton = layoutInflater.inflate(R.layout.folder_button, null)
                val folderList = LinearLayout(this)
                folderList.orientation = LinearLayout.VERTICAL
                folderList.visibility = View.GONE

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(padding, 0, 0, 0)
                folderList.layoutParams = params

                folderButton.findViewById<Button>(R.id.title).text = "${parts[i]}/"
                val parent = folderStack.lastOrNull()?.group ?: projectList
                parent.addView(folderButton)
                parent.addView(folderList)
                val folder = Folder(parts[i], folderList)
                folderStack.add(folder)
                folderButton.setOnClickListener {
                    folderList.visibility =
                        if (folderList.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                i++
            }

            val button = layoutInflater.inflate(R.layout.project_button, null)
            val parent = folderStack.lastOrNull()?.group ?: projectList
            parent.addView(button)

            button.findViewById<Button>(R.id.title).text = shortName
            button.setOnClickListener {
                projectName = project
                loadCurrentProject()
                flipper.displayedChild = 2
            }
            button.setOnLongClickListener {
                var dialog: AlertDialog? = null
                val builder = AlertDialog.Builder(this)
                val v = layoutInflater.inflate(R.layout.dialog_project_options, null)
                val nameInput = v.findViewById<EditText>(R.id.projectNameInput)
                nameInput.setText(project)
                v.findViewById<View>(R.id.copyButton)
                    .setOnClickListener {
                        val newName = nameInput.text.toString()
                            .trim().replace('\\', '/')
                        val error = getNameError(newName)
                        if (error == null) {
                            projectName = project
                            loadCurrentProject()
                            projectNames.add(newName)
                            projectNames.sortBy { it.lowercase() }
                            projectName = newName
                            save()
                            addAllProjectButtons(projectList)
                            dialog?.dismiss()
                        } else toast(error, false)
                    }
                v.findViewById<View>(R.id.deleteButton)
                    .setOnClickListener {
                        dialog?.dismiss()
                        val dialog1 = AlertDialog.Builder(this)
                        dialog1.setTitle("Delete Project '$project'?")
                        dialog1.setPositiveButton("Delete Project") { _, _ ->
                            deleteProject(project)
                            addAllProjectButtons(projectList)
                        }
                        dialog1.setNegativeButton("Cancel") { _, _ -> }
                        dialog1.show()
                    }
                v.findViewById<View>(R.id.renameButton)
                    .setOnClickListener {
                        val newName = nameInput.text.toString()
                            .trim().replace('\\', '/')
                        val error = getNameError(newName)
                        if (error == null) {
                            projectName = project
                            loadCurrentProject()
                            projectNames.remove(project)
                            projectNames.add(newName)
                            projectNames.sortBy { it.lowercase() }
                            projectName = newName
                            save()
                            addAllProjectButtons(projectList)
                            dialog?.dismiss()
                        } else toast(error, false)
                    }
                builder.setView(v)
                dialog = builder.show()
                true
            }
        }
        if (projectNames.isEmpty()) {
            val tv = TextView(this)
            tv.text = getString(R.string.no_projects_found)
            tv.gravity = Gravity.CENTER
            projectList.addView(tv)
        }
    }

    fun MainActivity.setupProjectList() {
        val projectList = findViewById<LinearLayout>(R.id.projectList)
        findViewById<View>(R.id.createProject).setOnClickListener {
            var dialog: AlertDialog? = null
            val builder = AlertDialog.Builder(this)
            val v = layoutInflater.inflate(R.layout.dialog_choose_name, null)
            val e = v.findViewById<EditText>(R.id.projectNameInput)
            v.findViewById<View>(R.id.createButton)
                .setOnClickListener {
                    val newName = e.text.toString()
                        .trim().replace('\\', '/')
                    val error = getNameError(newName)
                    if (error == null) {
                        CommandLogic.sequence.clear()
                        projectNames.add(newName)
                        projectNames.sortBy { it.lowercase() }
                        projectName = newName
                        save()
                        addAllProjectButtons(projectList)
                        loadCurrentProject()
                        dialog?.dismiss()
                        flipper.displayedChild = 2
                    } else toast(error, false)
                }
            builder.setView(v)
            dialog = builder.show()
        }
        addAllProjectButtons(projectList)
    }
}