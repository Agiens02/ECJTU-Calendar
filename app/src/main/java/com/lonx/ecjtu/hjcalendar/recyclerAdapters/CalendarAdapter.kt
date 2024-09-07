package com.lonx.ecjtu.hjcalendar.recyclerAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lonx.ecjtu.hjcalendar.R
import com.lonx.ecjtu.hjcalendar.utils.CourseInfo

class CalendarAdapter(
    private var courseList: List<CourseInfo>,
    private var onItemClickListener: OnItemClickListener?
) : RecyclerView.Adapter<CalendarAdapter.CourseViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(course: CourseInfo, position: Int)
    }

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.course_name)
        val classTime: TextView = itemView.findViewById(R.id.course_time)
        val classWeek: TextView = itemView.findViewById(R.id.course_week)
        val location: TextView = itemView.findViewById(R.id.course_location)
        val teacher: TextView = itemView.findViewById(R.id.course_teacher)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courseList[position]
        holder.courseName.text = course.courseName
        holder.classTime.text = course.classTime
        holder.classWeek.text = course.classWeek
        holder.location.text = course.location
        holder.teacher.text = course.teacher

        // 绑定点击事件
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(course, position)
        }
    }

    override fun getItemCount() = courseList.size

    fun updateData(newCourseList: List<CourseInfo>) {
        courseList = newCourseList
        notifyDataSetChanged()
    }
}
